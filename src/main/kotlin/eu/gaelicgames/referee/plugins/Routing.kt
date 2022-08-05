package eu.gaelicgames.referee.plugins

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.resources.Api
import eu.gaelicgames.referee.resources.Report
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.freemarker.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
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
                        TeamDEO.createFromTeam(it, addedTeams)
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
                            TournamentReports.referee eq user.id and (
                                    TournamentReports.isSubmitted eq false
                                    )
                        }.map { it }
                    }
                    val submittedReports = transaction {
                        TournamentReport.find {
                            TournamentReports.referee eq user.id and (
                                    TournamentReports.isSubmitted eq true
                                    )
                        }.map { it }
                    }
                    call.respond(
                        FreeMarkerContent(
                            "main_page.ftl",
                            mapOf(
                                "nonSubmittedReports" to nonSubmittedReports,
                                "submittedReports" to submittedReports
                            )
                        )
                    )
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }


            get<Report.New> {
                val resource =
                    this.javaClass.classLoader.getResource("static/edit_report.html")?.toURI()
                if (resource != null) {
                    val file = File(resource)
                    call.respondFile(file)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }

            }

            post<Api.NewTeam> {
                val newTeam = call.receiveOrNull<NewTeamDEO>()
                if (newTeam != null) {
                    val newTeamDB = transaction {
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
                            Team.find { Teams.id eq team.id }.firstOrNull()?.let {
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

            get<Api.Tournaments.FindByDate> { findByDate ->
                val date = LocalDate.parse(findByDate.date)
                val tournaments = transaction {
                    Tournament.find { Tournaments.date eq date }.map {
                        TournamentDEO.createFromTournament(it)
                    }
                }
                call.respond(tournaments)
            }

            post<Api.Tournaments.New> {
                val tournamentDraft = call.receiveOrNull<NewTournamentDEO>()
                if (tournamentDraft != null) {
                    val databaseTournament = transaction {
                        Tournament.new {
                            name = tournamentDraft.name
                            location = tournamentDraft.location
                            date = tournamentDraft.date
                        }.let { TournamentDEO.createFromTournament(it) }
                    }
                    call.respond(databaseTournament)
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }

            }

            get<Api.Codes> {
                val allCodes = transaction {
                    GameCode.all().map { GameCodeDEO(it) }
                }
                call.respond(allCodes)
            }
            post<Api.Reports.New> {
                val reportDraft = call.receiveOrNull<NewTournamentReportDEO>()
                if (reportDraft != null) {
                    val user = call.principal<UserPrincipal>()?.user
                    val report = user?.let {
                        reportDraft.storeInDatabase(it)
                    }
                    if (report != null) {
                        call.respond(report.id.value)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post<Api.Reports.Update> {
                val newReport = call.receiveOrNull<TournamentReportDEO>()?.updateInDatabase()
                if (newReport != null) {
                    call.respond(TournamentReportDEO.fromTournamentReport(newReport))
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }

            }

            post<Api.GameReports.New> {
                val gameReport = call.receiveOrNull<GameReportDEO>()
                if (gameReport != null) {
                    val createdReport = gameReport.createInDatabase()
                    if (createdReport.isSuccess) {
                        call.respond(GameReportDEO.fromGameReport(createdReport.getOrThrow()))
                    } else {
                        call.respond(
                            UpdateError(
                                UpdateErrorOptions.INSERTION_FAILED,
                                createdReport.exceptionOrNull()?.message?: "Unknown error"
                            )
                        )
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post<Api.GameReports.Update> {
                val gameReport = call.receiveOrNull<GameReportDEO>()
                if (gameReport != null) {
                    val updatedReport = gameReport.updateInDatabase()
                    if (updatedReport != null) {
                        call.respond(GameReportDEO.fromGameReport(updatedReport))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post<Api.GameReports.DisciplinaryAction.New> {
                TODO()
            }

            get<Api.Rules> {
                val rules = transaction {
                    Rule.all().map { RuleDEO(it) }
                }
                call.respond(rules)
            }

            get<Api.RulesForCode> { rules ->
                val code = transaction {
                    rules.code?.let {
                        GameCode.find { GameCodes.id eq it.toLong() }.firstOrNull()
                    }
                }
                val data = if (code != null) {
                    transaction {
                        Rule.find { Rules.code eq code.id }.map { RuleDEO(it) }
                    }
                } else {
                    transaction {
                        Rule.all().map { RuleDEO(it) }
                    }
                }
                call.respond(data)
            }

            get<Api.GameReportVariables> {
                call.respond(GameReportClasses.load())
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
