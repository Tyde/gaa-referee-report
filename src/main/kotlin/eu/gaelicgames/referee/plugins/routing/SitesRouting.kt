package eu.gaelicgames.referee.plugins.routing

import eu.gaelicgames.referee.data.*
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
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
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
    }
    authenticate("auth-session") {
        static("assets") {
            staticBasePackage = "static/assets"
            resources(".")
        }
        get("/") {
            val resource =
                this.javaClass.classLoader.getResourceAsStream("static/user_dashboard.html")
            if (resource != null) {
                call.respondOutputStream(contentType = ContentType.Text.Html) {
                    resource.copyTo(this)
                }
            } else {
                call.respondText("Resource not found",status = HttpStatusCode.InternalServerError)

            }
        }

        get("/legacy-home") {
            val user = call.principal<UserPrincipal>()?.user
            if (user != null) {
                val content = transaction {
                    val nonSubmittedReports = TournamentReports.innerJoin(Tournaments).select {
                        TournamentReports.referee eq user.id and (
                                TournamentReports.isSubmitted eq false
                                )
                    }.map { row ->
                        val tournament = Tournament.wrapRow(row)
                        TournamentReport.wrapRow(row) to tournament
                    }


                    val submittedReports =
                        TournamentReports.innerJoin(Tournaments).select {
                            TournamentReports.referee eq user.id and (
                                    TournamentReports.isSubmitted eq true
                                    )
                        }.map { row ->
                            val tournament = Tournament.wrapRow(row)
                            TournamentReport.wrapRow(row) to tournament
                        }

                    FreeMarkerContent(
                        "main_page.ftl",
                        mapOf(
                            "nonSubmittedReports" to nonSubmittedReports,
                            "submittedReports" to submittedReports
                        )
                    )
                }

                call.respond(
                    content

                )
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get<Report.New> {
            val resource =
                this.javaClass.classLoader.getResourceAsStream("static/edit_report.html")
            if (resource != null) {
                call.respondOutputStream(contentType = ContentType.Text.Html) {
                    resource.copyTo(this)
                }
            } else {
                call.respondText("Resource not found",status = HttpStatusCode.InternalServerError)

            }

        }

        get<Report.Edit> { edit ->
            val reportExists = transaction {
                TournamentReport.findById(edit.id) != null
            }
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
            if(resource != null) {
                if(reportExists) {
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
    authenticate ("admin-session") {
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