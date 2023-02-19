package eu.gaelicgames.referee.plugins.routing

import eu.gaelicgames.referee.data.Session
import eu.gaelicgames.referee.data.TournamentReport
import eu.gaelicgames.referee.data.UserPrincipal
import eu.gaelicgames.referee.data.UserSession
import eu.gaelicgames.referee.resources.Report
import eu.gaelicgames.referee.resources.UserRes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.freemarker.*
import io.ktor.server.http.content.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

fun Route.sites() {
    get("/login") {
        call.respond(FreeMarkerContent("login.ftl", null))
    }

    authenticate("auth-form") {
        post("/login") {
            val thisUser = call.principal<UserPrincipal>()
            println(thisUser)
            thisUser?.let { userPrincipal ->
                println("redirect!")
                val generatedUUID = UUID.randomUUID()
                transaction {
                    Session.new {
                        user = userPrincipal.user
                        uuid = generatedUUID
                        expires = LocalDateTime.now().plusDays(1)
                    }
                }
                call.sessions.set(UserSession(generatedUUID))
                call.respondRedirect("/")
            }
        }
    }
    static("/static") {
        staticBasePackage = "files_static"
        resources(".")
        //TODO: add .well-known
    }
    static("assets") {
        staticBasePackage = "static/assets"
        resources(".")
    }
    authenticate("auth-session") {

        get("/") {
            val resource =
                this.javaClass.classLoader.getResourceAsStream("static/user_dashboard.html")
            if (resource != null) {
                call.respondOutputStream(contentType = ContentType.Text.Html) {
                    resource.copyTo(this)
                }
            } else {
                call.respondText("Resource not found", status = HttpStatusCode.InternalServerError)

            }
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/login")
        }


        get<Report.New> {
            val resource =
                this.javaClass.classLoader.getResourceAsStream("static/edit_report.html")
            if (resource != null) {
                call.respondOutputStream(contentType = ContentType.Text.Html) {
                    resource.copyTo(this)
                }
            } else {
                call.respondText("Resource not found", status = HttpStatusCode.InternalServerError)

            }

        }

        get<Report.Edit> { edit ->
            val resource =
                this.javaClass.classLoader.getResourceAsStream("static/edit_report.html")
            if (resource != null) {
                call.respondOutputStream(contentType = ContentType.Text.Html) {
                    resource.copyTo(this)
                }
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }

        }

        get<Report.Show> { show ->
            val reportExists = transaction {
                TournamentReport.findById(show.id) != null
            }
            val resource =
                this.javaClass.classLoader.getResourceAsStream("static/show_report.html")
            if (resource != null) {
                if (reportExists) {
                    call.respondOutputStream(contentType = ContentType.Text.Html) {
                        resource.copyTo(this)
                    }
                } else {
                    call.respondText(
                        "Tried to show a report that does not exist",
                        status = HttpStatusCode.NotFound
                    )
                }

            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }

        }
    }
    authenticate("admin-session") {
        get("/admin/{...}") {
            val resource =
                this.javaClass.classLoader.getResourceAsStream("static/admin.html")
            if (resource != null) {
                call.respondOutputStream(contentType = ContentType.Text.Html) {
                    resource.copyTo(this)
                }
            } else {
                call.respond(HttpStatusCode.InternalServerError)
                error("Resource not found")
            }
        }
    }

    get<UserRes.Activate> {
        val resource =
            this.javaClass.classLoader.getResourceAsStream("static/onboarding.html")
        if (resource != null) {
            call.respondOutputStream(contentType = ContentType.Text.Html) {
                resource.copyTo(this)
            }
        } else {
            call.respond(HttpStatusCode.InternalServerError)
            error("Resource not found")
        }

    }
}