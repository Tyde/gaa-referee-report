package eu.gaelicgames.referee.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.data.api.TeamDEO
import eu.gaelicgames.referee.plugins.routing.adminApiRouting
import eu.gaelicgames.referee.plugins.routing.refereeApiRouting
import eu.gaelicgames.referee.plugins.routing.sites
import eu.gaelicgames.referee.resources.Api
import eu.gaelicgames.referee.util.JWTUtil
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new") {
    @Serializable
    @Resource("new")
    class New(val parent: Articles = Articles())

    @Serializable
    @Resource("{id}")
    class Id(val parent: Articles = Articles(), val id: Long) {
        @Serializable
        @Resource("edit")
        class Edit(val parent: Id)
    }
}

fun Application.configureRouting() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    install(Resources)
    install(StatusPages) {
        exception<AuthenticationException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, _ ->
            call.respond(HttpStatusCode.Forbidden)
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondText(text = "404: Page Not Found", status = status)
            println(call.request.uri)
        }

    }
    routing {
        sites()



        get<Api.TeamsAvailable> {
            call.respond(transaction {
                Team.all().map {
                    if (!it.isAmalgamation) {
                        TeamDEO.fromTeam(it)
                    } else {
                        val addedTeams = Amalgamation.find { Amalgamations.amalgamation eq it.id }.map { amlgm ->
                            TeamDEO.fromTeam(amlgm.addedTeam)
                        }
                        TeamDEO.fromTeam(it, addedTeams)
                    }
                }
            })
        }

        post<Api.Login> {
            try {
                val login = call.receive<LoginDEO>()
                println(login)
                val user = login.validate()
                if (user.isSuccess) {
                    val publicKey = JWTUtil.publicKey
                    val privateKey = JWTUtil.privateKey
                    val token = JWT.create()
                        .withAudience(JWTUtil.refereeAudience)
                        .withIssuer(JWTUtil.issuer)
                        .withClaim(JWTUtil.userClaimKey, user.getOrThrow().id.value)
                        .withExpiresAt(Date.from(Instant.now().plus(JWTUtil.expirationDuration)))
                        .sign(Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey))
                    call.respond(hashMapOf("token" to token))
                } else {
                    call.respond(
                        ApiError(
                            ApiErrorOptions.NOT_AUTHORIZED,
                            user.exceptionOrNull()?.message ?: "Unknown Error"
                        )
                    )
                }
            } catch (e: ContentTransformationException) {
                call.respond(
                    ApiError(
                        ApiErrorOptions.NOT_FOUND,
                        e.message ?: "Unknown Error"
                    )
                )
            }
        }


        authenticate("auth-session") {
            refereeApiRouting()
        }

        authenticate("admin-session") {
            adminApiRouting()
        }

        post<Api.User.ValidateActivationToken> {
            //This has to be outside any authentication context as user is still in the process of being created
            val token = kotlin.runCatching {
                call.receive<TokenDEO>()
            }
            if (token.isSuccess) {
                val tokenDEO = token.getOrThrow()
                val user = tokenDEO.validate()
                if (user.isSuccess) {
                    call.respond(RefereeDEO.fromReferee(user.getOrThrow()))
                } else {
                    call.respond(
                        ApiError(
                            ApiErrorOptions.INSERTION_FAILED,
                            user.exceptionOrNull()?.message ?: "Unknown Error"
                        )
                    )
                }
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.INSERTION_FAILED,
                        token.exceptionOrNull()?.message ?: "Unknown Error"
                    )
                )
            }
        }
        post<Api.User.Activate> {
            val token = kotlin.runCatching {
                call.receive<NewPasswordByTokenDEO>()
            }
            if (token.isSuccess) {
                val update = token.getOrThrow()
                val user = update.updatePassword()
                if (user.isSuccess) {
                    call.respond(RefereeDEO.fromReferee(user.getOrThrow()))
                } else {
                    call.respond(
                        ApiError(
                            ApiErrorOptions.INSERTION_FAILED,
                            user.exceptionOrNull()?.message ?: "Unable to update password"
                        )
                    )
                }
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.INSERTION_FAILED,
                        token.exceptionOrNull()?.message ?: "Unknown Error"
                    )
                )
            }
        }
        get("/*") {
            call.respondText("Catchall")
        }
    }

}


suspend inline fun <reified T : Any> PipelineContext<Unit, ApplicationCall>.receiveAndHandleDEO(onReceived: (T) -> Any) {
    val deo = runCatching { call.receive<T>() }
    if (deo.isSuccess) {
        call.respond(onReceived(deo.getOrThrow()))
    } else {
        call.respond(
            ApiError(
                ApiErrorOptions.NOT_FOUND,
                deo.exceptionOrNull()?.message ?: "Could not parse ${T::class.simpleName}"
            )
        )
    }
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
