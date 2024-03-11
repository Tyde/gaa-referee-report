package eu.gaelicgames.referee.plugins

import at.favre.lib.crypto.bcrypt.BCrypt
import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.JWTUtil
import eu.gaelicgames.referee.util.lockedTransaction
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
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
                println(credential)
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
                //val hash = BCrypt.withDefaults().hash(12, pw.toCharArray())

                val user = lockedTransaction {
                    val foundUser = User.find { Users.mail.lowerCase() eq credentials.name.lowercase() }
                    var loginUser: User? = null
                    for (user in foundUser) {
                        if (user.password.isEmpty()) {
                            this@validate.respond(HttpStatusCode.Unauthorized, "" +
                                    "User has no password set. Please use the activation " +
                                    "link from your mails. If your activation link has expired, " +
                                    "please contact the it officer.")
                            return@lockedTransaction null
                        }
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
            challenge {

                call.respondRedirect("/login?invalidCredentials")
            }
        }
        session<UserSession>("auth-session") {
            validate { session ->
                println("Validating session")
                Session.validateSession(session.uuid).map {
                    lockedTransaction { User.findById(it.user.id) }
                        ?.let { UserPrincipal(it) }
                }.getOrNull()


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
                Session.validateSession(session.uuid).map {
                    lockedTransaction { User.findById(it.user.id) }
                        ?.let {
                            if(it.role == UserRole.ADMIN) {
                                UserPrincipal(it)
                            } else {
                                null
                            }
                        }
                }.getOrNull()
                /*
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
                }*/
            }
            challenge {
                println("Challenge")
                call.respondRedirect("/login")
            }
        }

        session<UserSession>("ccc-session") {
            validate { session ->
                Session.validateSession(session.uuid).map {
                    lockedTransaction { User.findById(it.user.id) }
                        ?.let {
                            if(it.role == UserRole.CCC ||
                                it.role == UserRole.REFEREE ||
                                it.role == UserRole.ADMIN) {
                                UserPrincipal(it)
                            } else {
                                null
                            }
                        }
                }.getOrNull()
                /*
                transaction {
                    val foundSessions = Session.find { eu.gaelicgames.referee.data.Sessions.uuid eq session.uuid }
                    if (!foundSessions.empty()) {
                        val foundSession = foundSessions.first()
                        if (foundSession.user.role == UserRole.CCC ||
                            foundSession.user.role == UserRole.REFEREE ||
                            foundSession.user.role == UserRole.ADMIN) {
                            UserPrincipal(foundSession.user)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }*/
            }
            challenge {
                println("Challenge")
                call.respondRedirect("/login")
            }
        }


    }
}

