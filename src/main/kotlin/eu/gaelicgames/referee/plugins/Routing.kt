package eu.gaelicgames.referee.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eu.gaelicgames.referee.data.ApiError
import eu.gaelicgames.referee.data.ApiErrorOptions
import eu.gaelicgames.referee.data.api.*
import eu.gaelicgames.referee.plugins.routing.*
import eu.gaelicgames.referee.resources.Api
import eu.gaelicgames.referee.util.JWTUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.*


fun Application.configureRouting() {
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
        publicApiRouting()




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

        authenticate("ccc-session") {
            CCCApiRouting()
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
