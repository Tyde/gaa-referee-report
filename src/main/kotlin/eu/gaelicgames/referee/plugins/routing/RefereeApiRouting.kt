package  eu.gaelicgames.referee.plugins.routing

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.data.api.*
import eu.gaelicgames.referee.plugins.receiveAndHandleDEO
import eu.gaelicgames.referee.resources.Api
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.resources.post
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

fun Route.refereeApiRouting() {

    get<Api.Session> {
        val user = call.principal<UserPrincipal>()
        if (user != null) {
            call.respond(
                SessionInfoDEO.fromUser(
                    transaction {
                        User.findById(user.user.id)
                    }!!
                )
            )
        } else {
            call.respond(HttpStatusCode.Unauthorized)
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

    get<Api.Reports.My> {
        val user = call.principal<UserPrincipal>()
        if (user != null) {
            call.respond(transaction {
                val reports = TournamentReport.find { TournamentReports.referee eq user.user.id }
                reports.map { CompactTournamentReportDEO.fromTournamentReport(it) }
            })
        } else {
            call.respond(ApiError(ApiErrorOptions.NOT_FOUND, "User not logged in"))
        }
    }

    post<Api.Reports.UpdateAdditionalInformation> {
        val update = kotlin.runCatching { call.receive<UpdateReportAdditionalInformationDEO>()}
        if (update.isSuccess) {
            val updateResult = update.getOrThrow().updateInDatabase()
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
        receiveAndHandleDEO<NewTeamDEO> { newTeam ->
            val newTeamDB = transaction {
                Team.new {
                    name = newTeam.name
                    isAmalgamation = false
                }
            }
            TeamDEO.fromTeam(newTeamDB)
        }

    }

    post<Api.NewAmalgamation> {
        receiveAndHandleDEO<NewAmalgamationDEO> { newAmalgamation ->
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
            TeamDEO.fromTeam(newAmalgamationDB)
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
        receiveAndHandleDEO<NewTournamentDEO> { tournamentDraft ->
            transaction {
                Tournament.new {
                    name = tournamentDraft.name
                    location = tournamentDraft.location
                    date = tournamentDraft.date
                }.let { TournamentDEO.fromTournament(it) }
            }
        }

    }
    get<Api.Tournaments.All> {
        val tournaments = transaction {
            Tournament.all().sortedBy { it.date }.map { TournamentDEO.fromTournament(it) }
        }
        call.respond(tournaments)
    }

    get<Api.Codes> {
        val allCodes = transaction {
            GameCode.all().map { GameCodeDEO(it) }
        }
        call.respond(allCodes)
    }

    post<Api.Reports.New> {
        receiveAndHandleDEO<NewTournamentReportDEO> { reportDraft ->
            val user = call.principal<UserPrincipal>()?.user
            val report = user?.let {
                reportDraft.storeInDatabase(it)
            }
            if (report != null) {
                NewTournamentReportDEO.fromTournamentReport(report)
            } else {
                ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    "Report could not be stored in database"
                )
            }
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
        val reportDraft = kotlin.runCatching { call.receive<NewTournamentReportDEO>()}
        if (reportDraft.isSuccess) {
            val updatedReport = reportDraft.getOrThrow().updateInDatabase()
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
                    reportDraft.exceptionOrNull()?.message?:"Could not parse report input"
                )
            )
        }

    }

    post<Api.Reports.Submit> {
        val submitReport = kotlin.runCatching { call.receive<SubmitTournamentReportDEO>()}
        if (submitReport.isSuccess) {
            val report = submitReport.getOrThrow().submitInDatabase()
            if (report.isSuccess) {


                val response = SubmitTournamentReportDEO.fromTournamentReport(
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
                    submitReport.exceptionOrNull()?.message?:"Could not parse submit input"
                )
            )
        }
    }

    post<Api.Reports.Delete> {
        val deleteReport = kotlin.runCatching {
            call.receive<DeleteTournamentReportDEO>()
        }
        val user = call.principal<UserPrincipal>()?.user
        if (deleteReport.isSuccess && user != null) {

            val report = deleteReport.getOrThrow().deleteChecked(user)
            if (report.isSuccess) {
                call.respond(
                    deleteReport.getOrThrow()
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
                    "Could not parse delete input"
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
        val gameReport = kotlin.runCatching { call.receive<DeleteGameReportDEO>()}
        if(gameReport.isSuccess) {
            val res = gameReport.getOrThrow().deleteFromDatabase()
            if(res.isSuccess) {
                call.respond(gameReport.getOrThrow())
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.DELETE_FAILED,
                        res.exceptionOrNull()?.message ?: "Could not delete game report"
                    )
                )
            }
        } else {
            call.respond(
                ApiError(
                    ApiErrorOptions.DELETE_FAILED,
                    gameReport.exceptionOrNull()?.message ?: "Could not parse game report input"
                )
            )
        }
    }

    post<Api.GameReports.DisciplinaryAction.New> {
        handleDisciplinaryActionInput(doUpdate = false)
    }
    post<Api.GameReports.DisciplinaryAction.Update> {
        handleDisciplinaryActionInput(doUpdate = true)
    }
    post<Api.GameReports.DisciplinaryAction.Delete> {
        val disciplinaryAction = runCatching { call.receive<DeleteDisciplinaryActionDEO>() }
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
        val disciplinaryAction = runCatching { call.receive<DeleteInjuryDEO>() }
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
        val pitchReport = runCatching { call.receive<DeletePitchReportDEO>()}
        if(pitchReport.isSuccess) {
            val res = pitchReport.getOrThrow().deleteFromDatabase()
            if(res.isSuccess) {
                call.respond(pitchReport.getOrThrow())
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
            Rule.all().map { RuleDEO.fromRule(it) }
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
                Rule.find { Rules.code eq code.id }.map { RuleDEO.fromRule(it) }
            }
        } else {
            transaction {
                Rule.all().map { RuleDEO.fromRule(it) }
            }
        }
        call.respond(data)
    }

    get<Api.GameReportVariables> {
        call.respond(GameReportClassesDEO.load())
    }

    get<Api.PitchVariables> {
        call.respond(PitchVariablesDEO.load())
    }


    post<Api.GameType.New> {
        val gameType = runCatching { call.receive<GameTypeDEO>()}
        if (gameType.isSuccess) {
            val newGameType = gameType.getOrThrow().createInDatabase()
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

    post<Api.User.UpdateMe> {
        receiveAndHandleDEO<UpdateRefereeDAO> { dao ->
            dao.updateInDatabase().map {
                RefereeDEO.fromReferee(it)
            }.getOrElse { ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Could not update user") }

        }
    }

    post<Api.User.UpdatePassword> {
        receiveAndHandleDEO<UpdateRefereePasswordDAO> { dao ->
            dao.updateInDatabase()
                .getOrElse { ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Could not update user") }

        }
    }


}


private suspend fun PipelineContext<Unit, ApplicationCall>.handlePitchReportInput(doUpdate: Boolean) {
    val reportDraft = runCatching { call.receive<PitchReportDEO>()}
    if (reportDraft.isSuccess) {
        val updatedReport = if (doUpdate) {
            reportDraft.getOrThrow().updateInDatabase()
        } else {
            reportDraft.getOrThrow().createInDatabase()
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
                reportDraft.exceptionOrNull()?.message?:"Not able to parse pitch reports"
            )
        )
    }
}


private suspend fun PipelineContext<Unit, ApplicationCall>.handleGameReportInput(doUpdate: Boolean) {
    val gameReport = runCatching { call.receive<GameReportDEO>()}
    if (gameReport.isSuccess) {

        val updatedReport = if (doUpdate) {
            gameReport.getOrThrow().updateInDatabase()
        } else {
            gameReport.getOrThrow().createInDatabase()
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
                gameReport.exceptionOrNull()?.message?:"Not able to parse game report"
            )
        )
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleDisciplinaryActionInput(doUpdate: Boolean) {
    val dAction = runCatching { call.receive<DisciplinaryActionDEO>()}
    if (dAction.isSuccess) {

        val newDisciplinaryAction = if (doUpdate) {
            dAction.getOrThrow().updateInDatabase()
        } else {
            dAction.getOrThrow().createInDatabase()
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
                dAction.exceptionOrNull()?.message?:"Could not parse disciplinary action"
            )
        )
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleInjuryInput(doUpdate: Boolean) {
    val injury = runCatching { call.receive<InjuryDEO>()}
    if (injury.isSuccess) {

        val newInjury = if (doUpdate) {
            injury.getOrThrow().updateInDatabase()
        } else {
            injury.getOrThrow().createInDatabase()
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
                injury.exceptionOrNull()?.message?:"Could not parse injury"
            )
        )
    }
}

