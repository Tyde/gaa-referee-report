package eu.gaelicgames.referee.plugins.routing

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.data.api.CompleteReportDEO
import eu.gaelicgames.referee.data.api.fromTournamentReport
import eu.gaelicgames.referee.resources.Api
import eu.gaelicgames.referee.resources.Report
import eu.gaelicgames.referee.resources.UserRes
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.lockedTransaction
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.freemarker.*
import io.ktor.server.http.content.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import java.time.LocalDateTime
import java.util.*

fun Route.sites() {
    get("/login") {
        var invalidCredentials = false
        if (call.request.queryParameters["invalidCredentials"] != null) {
            invalidCredentials = true
        }
        call.respond(FreeMarkerContent("login.ftl", mapOf("isInvalidCredentialsCase" to invalidCredentials)))
    }

    authenticate("auth-form") {
        post("/login") {
            val thisUser = call.principal<UserPrincipal>()
            println(thisUser)
            if(thisUser != null) {
                println("redirect!")
                val generatedUUID = UUID.randomUUID()
                val session = lockedTransaction {
                    val dbSession = Session.new {
                        user = thisUser.user
                        uuid = generatedUUID
                        expires = LocalDateTime.now().plusDays(1)
                    }
                    SessionWithUserData.fromSession(dbSession)
                }
                CacheUtil.cacheSession(session)
                call.sessions.set(UserSession(generatedUUID))
                call.respondRedirect("/")
            } else {
                println("User incorrect. Redirecting to login")
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

    get<Report.Share> { share ->
        val uuid = UUID.fromString(share.uuid)
        val report = lockedTransaction {
            TournamentReportShareLink.find { TournamentReportShareLinks.uuid eq uuid }.firstOrNull()
        }
        if (report != null) {
            val resource =
                this.javaClass.classLoader.getResourceAsStream("static/show_report.html")
            if (resource != null) {
                call.respondOutputStream(contentType = ContentType.Text.Html) {
                    resource.copyTo(this)
                }
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        } else {
            call.respondText(
                "This share link does not exist or is expired",
                status = HttpStatusCode.NotFound
            )
        }

    }
    get<Api.Reports.GetShared> { getShared ->
        val uuid = UUID.fromString(getShared.uuid)
        val report  =  lockedTransaction {
           TournamentReportShareLink.find { TournamentReportShareLinks.uuid eq uuid }.firstOrNull()
               ?.let {
                CompleteReportDEO.fromTournamentReport(it.report)
            }
        }
        if (report != null) {
            call.respond(report)
        } else {
            call.respond(
                ApiError(ApiErrorOptions.NOT_FOUND, "This share link does not exist or is expired")
            )
        }
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
            val reportExists = lockedTransaction {
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

    get("/public") {
        val resource =
            this.javaClass.classLoader.getResourceAsStream("static/public_dashboard.html")
        if (resource != null) {
            call.respondOutputStream(contentType = ContentType.Text.Html) {
                resource.copyTo(this)
            }
        } else {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
