package eu.gaelicgames.referee.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import eu.gaelicgames.referee.resources.*
import io.ktor.server.auth.*
import io.ktor.server.freemarker.*

fun Application.configureRouting() {

    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden)
        }

    }
    install(Resources)
    routing {

        authenticate("auth-form") {
            get("/") {
                call.respond(FreeMarkerContent("main_page.ftl",null))
            }
            get<Report.New> {

            }
            /*post<Report.New> {

        }*/
            get<Report.Edit> { edit ->

            }
            get<Report.Edit.Game> { game ->

            }
        }
    }
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
