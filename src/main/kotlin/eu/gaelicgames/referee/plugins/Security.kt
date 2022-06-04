package eu.gaelicgames.referee.plugins

import at.favre.lib.crypto.bcrypt.BCrypt
import eu.gaelicgames.referee.data.*
import io.ktor.server.auth.*
import io.ktor.util.*
import io.ktor.server.sessions.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureSecurity() {

    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.extensions["SameSite"] = "lax"
            cookie.path = "/"
            cookie.maxAgeInSeconds = 60 * 60 * 24 * 7
        }
    }

    install(Authentication) {
        form("auth-form") {
            userParamName = "mail"
            passwordParamName = "password"
            validate { credentials ->
                val pw = credentials.password
                val hash = BCrypt.withDefaults().hash(12, pw.toCharArray())
                val user = transaction {
                    val foundUser = User.find { Users.mail eq credentials.name }
                    var loginUser: User? = null
                    for(user in foundUser) {
                        if(BCrypt.verifyer().verify(pw.toCharArray(),user.password).verified) {
                            loginUser = user
                        }
                    }
                    loginUser
                }
                if(user!=null) {
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
                    if(!foundSessions.empty()) {
                        val foundSession = foundSessions.first()
                        UserPrincipal(foundSession.user)
                    } else {
                        null
                    }
                }
            }
            challenge {
                call.respondRedirect("/login")
            }
        }
    }
}
