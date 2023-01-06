package  eu.gaelicgames.referee.plugins.routing;

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.data.api.*
import eu.gaelicgames.referee.resources.Api
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.resources.post
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

fun Route.refereeApiRouting() {

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
        if (update != null) {
            val updateResult = update.updateInDatabase()
            if (updateResult.isSuccess) {
                call.respond(
                    UpdateReportAdditionalInformationDEO.fromTournamentReportReport(
                        updateResult.getOrThrow()
                    )
                )
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.INSERTION_FAILED,
                        updateResult.exceptionOrNull()?.message ?: "Unknown error"
                    )
                )
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
        if (reportDraft != null) {
            val updatedReport = reportDraft.updateInDatabase()
            val report = updatedReport.getOrNull()
            if (updatedReport.isSuccess && report != null) {
                call.respond(
                    NewTournamentReportDEO.fromTournamentReport(
                        report
                    )
                )
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.INSERTION_FAILED,
                        updatedReport.exceptionOrNull()?.message ?: "Could not update report"
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

    post<Api.Reports.Submit> {
        val submitReport = call.receiveOrNull<SubmitTournamentDEO>()
        if (submitReport != null) {
            val report = submitReport.submitInDatabase()
            if (report.isSuccess) {


                val response = SubmitTournamentDEO.fromTournamentReport(
                    report.getOrThrow()
                )
                call.respond(
                    response
                )
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.INSERTION_FAILED,
                        report.exceptionOrNull()?.message?:"Unknown error"
                    )
                )
            }
        } else {
            call.respond(
                ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    "Could not parse submit input"
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

    post<Api.GameReports.Delete> {
        val gameReport = call.receiveOrNull<DeleteGameReportDEO>()
        if(gameReport!= null) {
            val res = gameReport.deleteFromDatabase()
            if(res.isSuccess) {
                call.respond(gameReport)
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.DELETE_FAILED,
                        res.exceptionOrNull()?.message ?: "Could not delete game report"
                    )
                )
            }
        }
    }

    post<Api.GameReports.DisciplinaryAction.New> {
        handleDisciplinaryActionInput(doUpdate = false)
    }
    post<Api.GameReports.DisciplinaryAction.Update> {
        handleDisciplinaryActionInput(doUpdate = true)
    }
    post<Api.GameReports.DisciplinaryAction.Delete> {
        val disciplinaryAction = call.runCatching { call.receive<DeleteDisciplinaryActionDEO>() }
        disciplinaryAction.getOrNull()?.apply {
            val res = deleteFromDatabase()
            if(res.isSuccess) {
                call.respond(this)
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.DELETE_FAILED,
                        res.exceptionOrNull()?.message ?: "Could not delete disciplinary action"
                    )
                )
            }
        } ?: call.respond(HttpStatusCode.BadRequest)

    }

    post<Api.GameReports.Injury.New> {
        handleInjuryInput(doUpdate = false)
    }
    post<Api.GameReports.Injury.Update> {
        handleInjuryInput(doUpdate = true)
    }
    post<Api.GameReports.Injury.Delete> {
        val disciplinaryAction = call.runCatching { call.receive<DeleteInjuryDEO>() }
        disciplinaryAction.getOrNull()?.apply {
            val res = deleteFromDatabase()
            if(res.isSuccess) {
                call.respond(this)
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.DELETE_FAILED,
                        res.exceptionOrNull()?.message ?: "Could not delete injury"
                    )
                )
            }
        } ?: call.respond(HttpStatusCode.BadRequest)

    }

    post<Api.Pitch.New> {
        handlePitchReportInput(doUpdate = false)
    }
    post<Api.Pitch.Update> {
        handlePitchReportInput(doUpdate = true)
    }
    post<Api.Pitch.Delete> {
        val pitchReport = call.receiveOrNull<DeletePitchReportDEO>()
        if(pitchReport!= null) {
            val res = pitchReport.deleteFromDatabase()
            if(res.isSuccess) {
                call.respond(pitchReport)
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.DELETE_FAILED,
                        res.exceptionOrNull()?.message ?: "Could not delete pitch report"
                    )
                )
            }
        }
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
                        newGameType.exceptionOrNull()?.message ?: "Could not create game type"
                    )
                )
            }
        } else {
            call.respond(HttpStatusCode.BadRequest)
        }
    }


}


private suspend fun PipelineContext<Unit, ApplicationCall>.handlePitchReportInput(doUpdate: Boolean) {
    val reportDraft = call.receiveOrNull<PitchReportDEO>()
    if (reportDraft != null) {
        val updatedReport = if (doUpdate) {
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

        val updatedReport = if (doUpdate) {
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

private suspend fun PipelineContext<Unit, ApplicationCall>.handleDisciplinaryActionInput(doUpdate: Boolean) {
    val dAction = call.receiveOrNull<DisciplinaryActionDEO>()
    if (dAction != null) {

        val newDisciplinaryAction = if (doUpdate) {
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

private suspend fun PipelineContext<Unit, ApplicationCall>.handleInjuryInput(doUpdate: Boolean) {
    val injury = call.receiveOrNull<InjuryDEO>()
    if (injury != null) {

        val newInjury = if (doUpdate) {
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