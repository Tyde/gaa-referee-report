package  eu.gaelicgames.referee.plugins.routing

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.data.api.*
import eu.gaelicgames.referee.plugins.AuthorizationException
import eu.gaelicgames.referee.plugins.receiveAndHandleDEO
import eu.gaelicgames.referee.plugins.respondResult
import eu.gaelicgames.referee.resources.Api
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.lockedTransaction
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate

fun Route.refereeApiRouting() {

    get<Api.Session> {
        val user = call.principal<UserPrincipal>()
        if (user != null) {
            call.respond(
                RefereeWithRoleDEO.fromUser(
                    lockedTransaction {
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
            val report = CacheUtil.getCachedReport(reportId).recoverCatching {
                println("Report $reportId not found in cache, loading from db")
                CompleteReportDEO.fromTournamentReportId(reportId).getOrThrow()
            }

            if (report.isSuccess) {
                val user = call.principal<UserPrincipal>()
                val report = report.getOrThrow()
                if (user != null) {
                    respondResult(
                        limitAccess(
                            user,
                            report,
                            isUserAllowedPredicate = { userIt, reportIt -> reportIt.referee.id == userIt.user.id.value },
                            isCCCAllowedPredicate = { _, reportIt -> reportIt.isSubmitted },
                            customUserDisallowedMessage = "Report can only be accessed by the referee who created it",
                            customCCCDisallowedMessage = "Report can only be accessed by CCC after submission",
                        ) { it }
                    )
                } else {
                    call.respond(
                        ApiError(
                            ApiErrorOptions.NOT_FOUND,
                            "User not logged in"
                        )
                    )
                }
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.NOT_FOUND,
                        report.exceptionOrNull()?.message ?: "Unknown error"
                    )
                )
                report.exceptionOrNull()?.printStackTrace()
            }

        } else {
            call.respond(ApiError(ApiErrorOptions.NOT_FOUND, "Report id must be positive"))
        }
    }

    get<Api.Reports.My> {
        val user = call.principal<UserPrincipal>()
        if (user != null) {
            call.respond(newSuspendedTransaction {
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
            CacheUtil.deleteCachedTeamList()
            val existingTeamWithSameName = lockedTransaction {
                Team.find { Teams.name eq newTeam.name }.firstOrNull()
            }
            if (existingTeamWithSameName != null) {
                return@receiveAndHandleDEO ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    "Team with same name already exists"
                )
            }
            val newTeamDB = lockedTransaction {
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
            newAmalgamation.createInDatabase().map {
                TeamDEO.fromTeam(it)
            }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    get<Api.Tournaments.FindByDate> { findByDate ->
        val date = LocalDate.parse(findByDate.date)
        val tournaments = newSuspendedTransaction {
            Tournament.find { Tournaments.date eq date or (
                    Tournaments.date lessEq date and (Tournaments.endDate greaterEq date)
                and (Tournaments.isLeague eq true)
            ) }.map {
                TournamentDEO.fromTournament(it)
            }
        }
        call.respond(tournaments)
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
                deo.deleteChecked(user).map { deo }.getOrElse {
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
        receiveAndHandleDEO<DeleteGameReportDEO> { deo ->
            deo.deleteChecked(user).map { deo }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.GameReports.DisciplinaryAction.New> {
        println("new DA:" + Thread.currentThread().name)
        handleDisciplinaryActionInput(doUpdate = false)
    }
    post<Api.GameReports.DisciplinaryAction.Update> {
        println("Update DA:" + Thread.currentThread().name)

        handleDisciplinaryActionInput(doUpdate = true)
    }
    post<Api.GameReports.DisciplinaryAction.Delete> {
        val user = call.principal<UserPrincipal>()?.user!!
        receiveAndHandleDEO<DeleteDisciplinaryActionDEO> { deo ->
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
        val code = lockedTransaction {
            rules.code?.let {
                GameCode.find { GameCodes.id eq it.toLong() }.firstOrNull()
            }
        }
        val data = if (code != null) {
            newSuspendedTransaction {
                Rule.find { Rules.code eq code.id }.map { RuleDEO.fromRule(it) }
            }
        } else {
            newSuspendedTransaction {
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

    post<Api.Tournaments.PreselectedTeams.Get> {
        receiveAndHandleDEO<GetTournamentTeamPreselectionDEO> { deo ->
            deo.load().getOrElse {
                ApiError(ApiErrorOptions.NOT_FOUND, it.message ?: "Could not load preselected teams")
            }
        }
    }

}


private suspend fun PipelineContext<Unit, ApplicationCall>.handlePitchReportInput(doUpdate: Boolean) {

    receiveAndHandleDEO<PitchReportDEO> { pitchDEO ->
        val principal = call.principal<UserPrincipal>()!!
        val out = limitAccess(
            principal,
            pitchDEO,
            isUserAllowedPredicate = { user, pitchReportDEO -> pitchReportDEO.getRefereeId() == user.user.id.value },
            customUserDisallowedMessage = "Pitch report can can only be edited by the referee who created the report"
        ) { pitchReportDEO ->
            val updatedReport = if (doUpdate) {
                pitchReportDEO.updateInDatabase()
            } else {
                pitchReportDEO.createInDatabase()
            }
            updatedReport.map { PitchReportDEO.fromPitchReport(it) }.getOrThrow()
        }
        out.getOrThrow()
    }

}


private suspend fun PipelineContext<Unit, ApplicationCall>.handleGameReportInput(doUpdate: Boolean) {
    receiveAndHandleDEO<GameReportDEO> { deo ->
        val principal = call.principal<UserPrincipal>()!!
        limitAccess(
            principal,
            deo,
            isUserAllowedPredicate = { user, gameReportDEO -> gameReportDEO.getRefereeId() == user.user.id.value },
            customUserDisallowedMessage = "Game report can can only be edited by the referee who created the report"
        ) { gameReportDEO ->
            val updatedReport = if (doUpdate) {
                gameReportDEO.updateInDatabase()
            } else {
                gameReportDEO.createInDatabase()
            }
            updatedReport.map { GameReportDEO.fromGameReport(it) }.getOrThrow()
        }.getOrThrow()
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleDisciplinaryActionInput(doUpdate: Boolean) {
    receiveAndHandleDEO<DisciplinaryActionDEO> { deo ->
        val principal = call.principal<UserPrincipal>()!!
        limitAccess(
            principal,
            deo,
            isUserAllowedPredicate = { user, disciplinaryActionDEO -> disciplinaryActionDEO.getRefereeId() == user.user.id.value },
            customUserDisallowedMessage = "Disciplinary action can can only be edited by the referee who created the report"
        ) { disciplinaryActionDEO ->


            val updatedReport = if (doUpdate) {
                deo.updateInDatabase()
            } else {
                deo.createInDatabase()
            }
            updatedReport.map { DisciplinaryActionDEO.fromDisciplinaryAction(it) }.getOrThrow()
        }.getOrThrow()
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleInjuryInput(doUpdate: Boolean) {
    receiveAndHandleDEO<InjuryDEO> { deo ->
        val principal = call.principal<UserPrincipal>()!!
        limitAccess(
            principal,
            deo,
            isUserAllowedPredicate = { user, injuryDEO -> injuryDEO.getRefereeId() == user.user.id.value },
            customUserDisallowedMessage = "Injury can can only be edited by the referee who created the report"
        ) { injuryDEO ->
            val updatedReport = if (doUpdate) {
                deo.updateInDatabase()
            } else {
                deo.createInDatabase()
            }
            updatedReport.map { InjuryDEO.fromInjury(it) }.getOrThrow()
        }.getOrThrow()
    }
}


private suspend inline fun <reified T : Any, reified R : Any> limitAccess(
    user: UserPrincipal,
    serializableObject: T,
    isUserAllowedPredicate: (UserPrincipal, T) -> Boolean,
    isCCCAllowedPredicate: (UserPrincipal, T) -> Boolean = { _, _ -> false },
    customUserDisallowedMessage: String = "Data can only be accessed by the referee who created it",
    customCCCDisallowedMessage: String = "Data can only be accessed by CCC after submission",
    mapIfAllowed: (T) -> R
): Result<R> {

    val role = user.user.role
    return if (role == UserRole.ADMIN) {
        Result.success(mapIfAllowed(serializableObject))
    } else if (role == UserRole.CCC) {
        if (isCCCAllowedPredicate(user, serializableObject)) {
            Result.success(mapIfAllowed(serializableObject))
        } else {
            Result.failure(AuthorizationException(customCCCDisallowedMessage))
        }
    } else if (role == UserRole.REFEREE_AND_CCC) {
        if (isUserAllowedPredicate(user, serializableObject) ||
            isCCCAllowedPredicate(user, serializableObject)) {
            Result.success(mapIfAllowed(serializableObject))
        } else {
            Result.failure(AuthorizationException(customUserDisallowedMessage))
        }
    } else {
        if (isUserAllowedPredicate(user, serializableObject)) {
            Result.success(mapIfAllowed(serializableObject))
        } else {
            Result.failure(AuthorizationException(customUserDisallowedMessage))
        }
    }
}
