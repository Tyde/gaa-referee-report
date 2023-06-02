package  eu.gaelicgames.referee.plugins.routing

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.data.api.*
import eu.gaelicgames.referee.plugins.receiveAndHandleDEO
import eu.gaelicgames.referee.resources.Api
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
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
                RefereeWithRoleDEO.fromUser(
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
        receiveAndHandleDEO<UpdateReportAdditionalInformationDEO> { deo ->
            deo.updateInDatabase().map {
                UpdateReportAdditionalInformationDEO.fromTournamentReportReport(it)
            }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
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

    get<Api.Regions.All> {
        val regions = transaction {
            Region.all().map { RegionDEO.fromRegion(it) }
        }
        call.respond(regions)
    }

    post<Api.Tournaments.New> {
        receiveAndHandleDEO<NewTournamentDEO> { tournamentDraft ->
            tournamentDraft.storeInDatabase().map {
                TournamentDEO.fromTournament(it)
            }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }

    }
    get<Api.Tournaments.All> {
        val tournaments = transaction {
            Tournament.all().sortedBy { it.date }.map { TournamentDEO.fromTournament(it) }
        }
        call.respond(tournaments)
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
        receiveAndHandleDEO<NewTournamentReportDEO> { newTournamentReportDEO ->
            newTournamentReportDEO.updateInDatabase().map {
                NewTournamentReportDEO.fromTournamentReport(it)
            }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.Reports.Submit> {
        receiveAndHandleDEO<TournamentReportByIdDEO> { deo ->
            deo.submitInDatabase().map {
                TournamentReportByIdDEO.fromTournamentReport(it)
            }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.Reports.Delete> {
        val user = call.principal<UserPrincipal>()?.user
        if (user != null) {
            receiveAndHandleDEO<DeleteTournamentReportDEO> { deo ->
                deo.deleteChecked(user).map{deo}.getOrElse {
                    ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
                }
            }
        } else {
            call.respond(ApiError(ApiErrorOptions.NOT_FOUND, "User not logged in"))
        }
    }

    post<Api.Reports.Share> {
        receiveAndHandleDEO<TournamentReportByIdDEO> { deo ->
            deo.createShareLink().getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.GameReports.New> {
        handleGameReportInput(doUpdate = false)
    }

    post<Api.GameReports.Update> {
        handleGameReportInput(doUpdate = true)
    }

    post<Api.GameReports.Delete> {
        val user = call.principal<UserPrincipal>()?.user!!
        receiveAndHandleDEO<DeleteGameReportDEO> {  deo ->
            deo.deleteChecked(user).map{deo}.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
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
        val user = call.principal<UserPrincipal>()?.user!!
        receiveAndHandleDEO<DeleteDisciplinaryActionDEO> {  deo ->
            deo.deleteChecked(user).map { deo }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }

    }

    post<Api.GameReports.Injury.New> {
        handleInjuryInput(doUpdate = false)
    }
    post<Api.GameReports.Injury.Update> {
        handleInjuryInput(doUpdate = true)
    }
    post<Api.GameReports.Injury.Delete> {
        val user = call.principal<UserPrincipal>()?.user!!
        receiveAndHandleDEO<DeleteInjuryDEO> { deo ->
            deo.deleteChecked(user).map { deo }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.Pitch.New> {
        handlePitchReportInput(doUpdate = false)
    }
    post<Api.Pitch.Update> {
        handlePitchReportInput(doUpdate = true)
    }
    post<Api.Pitch.Delete> {
        val user = call.principal<UserPrincipal>()?.user!!
        receiveAndHandleDEO<DeletePitchReportDEO> { deo ->
            deo.deleteChecked(user).map { deo }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
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




    post<Api.GameType.New> {
        receiveAndHandleDEO<GameTypeDEO> { deo ->
            deo.createInDatabase().map { GameTypeDEO.fromGameType(it) }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
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
    receiveAndHandleDEO<PitchReportDEO> { pitchReportDEO ->
        val updatedReport = if (doUpdate) {
            pitchReportDEO.updateInDatabase()
        } else {
            pitchReportDEO.createInDatabase()
        }
        updatedReport.map { PitchReportDEO.fromPitchReport(it) }.getOrElse {
            ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
        }
    }

}


private suspend fun PipelineContext<Unit, ApplicationCall>.handleGameReportInput(doUpdate: Boolean) {
    receiveAndHandleDEO<GameReportDEO> { deo ->
        val updatedReport = if (doUpdate) {
            deo.updateInDatabase()
        } else {
            deo.createInDatabase()
        }
        updatedReport.map { GameReportDEO.fromGameReport(it) }.getOrElse {
            ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleDisciplinaryActionInput(doUpdate: Boolean) {
    receiveAndHandleDEO<DisciplinaryActionDEO> { deo ->
        val updatedReport = if (doUpdate) {
            deo.updateInDatabase()
        } else {
            deo.createInDatabase()
        }
        updatedReport.map { DisciplinaryActionDEO.fromDisciplinaryAction(it) }.getOrElse {
            ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleInjuryInput(doUpdate: Boolean) {
    receiveAndHandleDEO<InjuryDEO> {  deo ->
        val updatedReport = if (doUpdate) {
            deo.updateInDatabase()
        } else {
            deo.createInDatabase()
        }
        updatedReport.map { InjuryDEO.fromInjury(it) }.getOrElse {
            ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
        }
    }
}

