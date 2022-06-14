package eu.gaelicgames.referee.plugins

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.data.Sessions.expires
import eu.gaelicgames.referee.data.Sessions.user
import eu.gaelicgames.referee.data.Sessions.uuid
import eu.gaelicgames.referee.resources.Api
import eu.gaelicgames.referee.resources.Report
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.resources.*
import io.ktor.server.auth.*
import io.ktor.server.freemarker.*
import io.ktor.server.plugins.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.resources.Resources
import io.ktor.server.routing.get
import io.ktor.server.sessions.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*
import kotlinx.serialization.*
import java.io.File


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
    install(Resources)
    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden)
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondText(text = "404: Page Not Found", status = status)
            println(call)
        }

    }
    routing {
        get("/login") {
            call.respond(FreeMarkerContent("login.ftl", null))
        }


        authenticate("auth-form") {
            post("/login") {
                val this_user = call.principal<UserPrincipal>()
                println(this_user)
                this_user?.let { it ->
                    println("redirect!")
                    val genereatedUUID = UUID.randomUUID()
                    transaction {
                        Session.new {
                            user = it.user
                            uuid = genereatedUUID
                            expires = LocalDateTime.now().plusDays(1)
                        }
                    }
                    call.sessions.set(UserSession(genereatedUUID))
                    call.respondRedirect("/")
                }
            }
        }
        get<Api.TeamsAvailable> {
            call.respond(transaction {
                Team.all().map {
                    if (!it.isAmalgamation) {
                        TeamDEO.createFromTeam(it)
                    } else {
                        val addedTeams = Amalgamation.find { Amalgamations.amalgamation eq it.id }.map { amlgm ->
                            TeamDEO.createFromTeam(amlgm.addedTeam)
                        }
                        TeamDEO.createFromTeam(it,addedTeams)
                    }
                }
            })
        }


        authenticate("auth-session") {
            static("assets") {
                staticBasePackage = "static/assets"
                resources(".")
            }
            get("/") {
                val user = call.principal<UserPrincipal>()?.user
                if (user != null) {
                    val nonSubmittedReports = transaction {
                        TournamentReport.find {
                            TournamentReports.referee eq user.id and(
                                    TournamentReports.isSubmitted eq false
                                    )
                        }.map { it }
                    }
                    val submittedReports = transaction {
                        TournamentReport.find {
                            TournamentReports.referee eq user.id and(
                                    TournamentReports.isSubmitted eq true
                                    )
                        }.map { it }
                    }
                    call.respond(FreeMarkerContent("main_page.ftl",
                            mapOf(
                                "nonSubmittedReports" to nonSubmittedReports,
                                "submittedReports" to submittedReports
                            )
                        ))
                }
                else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

            get<Report.New> {
                val resource =
                    this.javaClass.
                        classLoader.getResource("static/edit_report.html")?.toURI()
                if(resource != null) {
                    val file = File(resource)
                    call.respondFile(file)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }

            }

            post<Api.NewTeam> {
                val newTeam = call.receiveOrNull<NewTeamDEO>()
                if (newTeam != null) {
                    val newTeamDB = transaction{
                        Team.new {
                            name = newTeam.name
                            isAmalgamation = false
                        }
                    }
                    call.respond(TeamDEO.createFromTeam(newTeamDB))
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post<Api.NewAmalgamation> {
                val newAmalgamation = call.receiveOrNull<NewAmalgamationDEO>()
                if (newAmalgamation != null) {
                    val newAmalgamationDB = transaction {
                        val amalgamation_base = Team.new {
                            name = newAmalgamation.name
                            isAmalgamation = true
                        }
                        for (team in newAmalgamation.teams) {
                            Team.find{ Teams.id eq team.id}.firstOrNull()?.let {
                                Amalgamation.new {
                                    amalgamation = amalgamation_base
                                    addedTeam = it
                                }
                            }
                        }
                        amalgamation_base
                    }
                    call.respond(TeamDEO.createFromTeam(newAmalgamationDB))
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }



            /*post<Report.New> {

        }
            get<Report.Edit> { edit ->

            }
            get<Report.Edit.Game> { game ->

            }*/
        }
        get("/*") {
            call.respondText("Catchall")
        }
    }

}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
