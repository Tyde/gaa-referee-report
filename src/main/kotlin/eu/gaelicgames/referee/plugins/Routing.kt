package eu.gaelicgames.referee.plugins

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.data.api.*
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
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull.content
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
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
        exception<AuthenticationException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, _ ->
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
                this_user?.let { userPrincipal ->
                    println("redirect!")
                    val genereatedUUID = UUID.randomUUID()
                    transaction {
                        Session.new {
                            user = userPrincipal.user
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
                        TeamDEO.fromTeam(it)
                    } else {
                        val addedTeams = Amalgamation.find { Amalgamations.amalgamation eq it.id }.map { amlgm ->
                            TeamDEO.fromTeam(amlgm.addedTeam)
                        }
                        TeamDEO.fromTeam(it, addedTeams)
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
                    this.javaClass.classLoader.getResource("static/edit_report.html")?.toURI()
                if (resource != null) {
                    val file = File(resource)
                    call.respondFile(file)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }

            }

            get<Report.Edit> { edit ->
                val reportExists = transaction {
                    TournamentReport.findById(edit.id) != null
                }
                val resource =
                    this.javaClass.classLoader.getResource("static/edit_report.html")?.toURI()
                if (resource != null && reportExists) {
                    val file = File(resource)
                    call.respondFile(file)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }

            }

            get<Api.Reports.Get> { get ->
                val reportId = get.id
                if (reportId >= 0) {
                    call.respond(transaction {
                        val report = TournamentReport.findById(reportId)
                        if (report != null) {
                            CompleteReportDEO.fromTournamentReport(report)
                        } else {
                            ApiError(ApiErrorOptions.NOT_FOUND, "Report under given id not found")
                        }
                    })
                } else {
                    call.respond(ApiError(ApiErrorOptions.NOT_FOUND, "Report id must be positive"))
                }
            }

            post<Api.Reports.UpdateAdditionalInformation> {
                val update = call.receiveOrNull<UpdateReportAdditionalInformationDEO>()
                if(update!= null) {
                    val updateResult = update.updateInDatabase()
                    if(updateResult.isSuccess) {
                        call.respond(
                            UpdateReportAdditionalInformationDEO.fromTournamentReportReport(
                                updateResult.getOrThrow()
                            )
                        )
                    } else {
                        call.respond(
                            ApiError(
                                ApiErrorOptions.INSERTION_FAILED,
                                updateResult.exceptionOrNull()?.message ?: "Unknown error"))
                    }
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
                    call.respond(TeamDEO.fromTeam(newTeamDB))
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
                    call.respond(TeamDEO.fromTeam(newAmalgamationDB))
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            get<Api.Tournaments.FindByDate> { findByDate ->
                val date = LocalDate.parse(findByDate.date)
                val tournaments = transaction {
                    Tournament.find { Tournaments.date eq date }.map {
                        TournamentDEO.fromTournament(it)
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
                        }.let { TournamentDEO.fromTournament(it) }
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
                        call.respond(NewTournamentReportDEO.fromTournamentReport(report))
                    } else {
                        call.respond(
                            ApiError(
                                ApiErrorOptions.INSERTION_FAILED,
                                "Report could not be stored in database"
                            )
                        )
                    }
                } else {
                    call.respond(
                        ApiError(
                            ApiErrorOptions.INSERTION_FAILED,
                            "Could not parse report input"
                        )
                    )
                }
            }

            post<Api.Reports.Update> {
                /*
                val newReport = call.receiveOrNull<TournamentReportDEO>()?.updateInDatabase()
                if (newReport != null) {
                    call.respond(TournamentReportDEO.fromTournamentReport(newReport))
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }*/
                val reportDraft = call.receiveOrNull<NewTournamentReportDEO>()
                if (reportDraft!= null) {
                    val updatedReport = reportDraft.updateInDatabase()
                    val report = updatedReport.getOrNull()
                    if(updatedReport.isSuccess && report != null) {
                        call.respond(
                            NewTournamentReportDEO.fromTournamentReport(
                                report
                            )
                        )
                    } else {
                        call.respond(
                            ApiError(
                                ApiErrorOptions.INSERTION_FAILED,
                                updatedReport.exceptionOrNull()?.message?: "Could not update report"
                            )
                        )
                    }
                } else {
                    call.respond(
                        ApiError(
                            ApiErrorOptions.INSERTION_FAILED,
                            "Could not parse report input"
                        )
                    )
                }

            }

            post<Api.GameReports.New> {
                handleGameReportInput(doUpdate = false)
            }

            post<Api.GameReports.Update> {
                handleGameReportInput(doUpdate = true)
            }

            post<Api.GameReports.DisciplinaryAction.New> {
                handleDisciplinaryActionInput(doUpdate = false)
            }
            post<Api.GameReports.DisciplinaryAction.Update> {
                handleDisciplinaryActionInput(doUpdate = true)
            }

            post<Api.GameReports.Injury.New> {
                handleInjuryInput(doUpdate = false)
            }
            post<Api.GameReports.Injury.Update> {
                handleInjuryInput(doUpdate = true)
            }

            post<Api.Pitch.New> {
                handlePitchReportInput(doUpdate = false)
            }
            post<Api.Pitch.Update> {
                handlePitchReportInput(doUpdate = true)
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

            get<Api.PitchVariables> {
                call.respond(PitchVariablesDEO.load())
            }


            post<Api.GameType.New> {
                val gameType = call.receiveOrNull<GameTypeDEO>()
                if (gameType != null) {
                    val newGameType = gameType.createInDatabase()
                    if (newGameType.isSuccess) {
                        call.respond(GameTypeDEO.fromGameType(newGameType.getOrThrow()))
                    } else {
                        call.respond(
                            ApiError(
                                ApiErrorOptions.INSERTION_FAILED,
                                newGameType.exceptionOrNull()?.message?: "Could not create game type"
                            )
                        )
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

        }
        get("/*") {
            call.respondText("Catchall")
        }
    }

}

private suspend fun PipelineContext<Unit, ApplicationCall>.handlePitchReportInput(doUpdate: Boolean) {
    val reportDraft = call.receiveOrNull<PitchReportDEO>()
    if (reportDraft != null) {
        val updatedReport = if(doUpdate) {
            reportDraft.updateInDatabase()
        } else {
            reportDraft.createInDatabase()
        }
        if (updatedReport.isSuccess) {
            call.respond(
                PitchReportDEO.fromPitchReport(
                    updatedReport.getOrThrow()
                )
            )
        } else {
            call.respond(
                ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    updatedReport.exceptionOrNull()?.message ?: "Unknown error"
                )
            )
        }
    } else {
        call.respond(
            ApiError(
                ApiErrorOptions.INSERTION_FAILED,
                "Not able to parse pitch reports"
            )
        )
    }
}



private suspend fun PipelineContext<Unit, ApplicationCall>.handleGameReportInput(doUpdate: Boolean) {
    val gameReport = call.receiveOrNull<GameReportDEO>()
    if (gameReport != null) {

        val updatedReport = if(doUpdate) {
            gameReport.updateInDatabase()
        } else {
            gameReport.createInDatabase()
        }
        if (updatedReport.isSuccess) {
            call.respond(
                GameReportDEO.fromGameReport(
                    updatedReport.getOrThrow()
                )
            )
        } else {
            call.respond(
                ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    updatedReport.exceptionOrNull()?.message ?: "Unknown error"
                )
            )
        }
    } else {
        call.respond(
            ApiError(
                ApiErrorOptions.INSERTION_FAILED,
                "Not able to parse game report"
            )
        )
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleDisciplinaryActionInput(doUpdate:Boolean) {
    val dAction = call.receiveOrNull<DisciplinaryActionDEO>()
    if (dAction != null) {

        val newDisciplinaryAction = if(doUpdate) {
            dAction.updateInDatabase()
        } else {
            dAction.createInDatabase()
        }
        if (newDisciplinaryAction.isSuccess) {
            call.respond(
                DisciplinaryActionDEO.fromDisciplinaryAction(
                    newDisciplinaryAction.getOrThrow()
                )
            )
        } else {
            call.respond(
                ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    newDisciplinaryAction.exceptionOrNull()?.message ?: "Unknown error"
                )
            )
        }
    } else {
        call.respond(
            ApiError(
                ApiErrorOptions.INSERTION_FAILED,
                "Could not parse disciplinary action"
            )
        )
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleInjuryInput(doUpdate:Boolean) {
    val injury = call.receiveOrNull<InjuryDEO>()
    if (injury != null) {

        val newInjury = if(doUpdate) {
            injury.updateInDatabase()
        } else {
            injury.createInDatabase()
        }
        if (newInjury.isSuccess) {
            call.respond(
                InjuryDEO.fromInjury(
                    newInjury.getOrThrow()
                )
            )
        } else {
            call.respond(
                ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    newInjury.exceptionOrNull()?.message ?: "Unknown error"
                )
            )
        }
    } else {
        call.respond(
            ApiError(
                ApiErrorOptions.INSERTION_FAILED,
                "Could not parse injury"
            )
        )
    }
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
