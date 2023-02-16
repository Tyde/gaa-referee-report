package eu.gaelicgames.referee.plugins

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwk.JwkProviderBuilder
import com.typesafe.config.ConfigUtil
import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.GGERefereeConfig
import eu.gaelicgames.referee.util.JWTUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.TimeUnit
import kotlin.collections.first
import kotlin.collections.set

fun Application.configureSecurity() {

    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.extensions["SameSite"] = "lax"
            cookie.path = "/"
            cookie.maxAgeInSeconds = 60 * 60 * 24 * 7
        }
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = JWTUtil.realm
            verifier(JWTUtil.jwkProvider, JWTUtil.issuer) {
                acceptLeeway(3)
            }
            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }

        }
        form("auth-form") {
            userParamName = "mail"
            passwordParamName = "password"
            validate { credentials ->
                val pw = credentials.password
                val hash = BCrypt.withDefaults().hash(12, pw.toCharArray())

                val user = transaction {
                    val foundUser = User.find { Users.mail eq credentials.name }
                    var loginUser: User? = null
                    for (user in foundUser) {
                        if (BCrypt.verifyer().verify(pw.toCharArray(), user.password).verified) {
                            loginUser = user
                        }
                    }
                    loginUser
                }
                if (user != null) {
                    UserPrincipal(user)
                } else {
                    null
                }
            }
        }
        session<UserSession>("auth-session") {
            validate { session ->
                transaction {
                    val foundSessions = Session.find { eu.gaelicgames.referee.data.Sessions.uuid eq session.uuid }
                    if (!foundSessions.empty()) {
                        val foundSession = foundSessions.first()
                        UserPrincipal(foundSession.user)
                    } else {
                        null
                    }
                }
            }
            challenge {
                if(this.call.request.uri.startsWith("/api")) {
                    //We don't want to redirect when this is an api request. We just throw an api error
                    call.respond(ApiError(
                        error = ApiErrorOptions.NOT_AUTHORIZED,
                        message = "You are not authorized to call this api"
                    ))
                } else {
                    call.respondRedirect("/login")
                }
            }
        }
        session<UserSession>("admin-session") {
            validate { session ->
                transaction {
                    val foundSessions = Session.find { eu.gaelicgames.referee.data.Sessions.uuid eq session.uuid }
                    if (!foundSessions.empty()) {
                        val foundSession = foundSessions.first()
                        if (foundSession.user.role == UserRole.ADMIN) {
                            UserPrincipal(foundSession.user)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            }
            challenge {
                println("Challenge")
                call.respondRedirect("/login")
            }
        }


    }
}

