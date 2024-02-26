package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.CacheUtil
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*


suspend fun Transaction.clearCacheForTournamentReport(report: TournamentReport) {
    val tournamentID = report.tournament.id.value
    CacheUtil.deleteCachedCompleteTournamentReport(tournamentID)
    CacheUtil.deleteCachedPublicTournamentReport(tournamentID)
    CacheUtil.deleteCachedReport(report.id.value)

}

suspend fun CompleteReportDEO.Companion.fromTournamentReport(
    tournamentReport: TournamentReport
): CompleteReportDEO = coroutineScope {
    val report = transaction {
        val report = CompleteReportDEO(
            id = tournamentReport.id.value,
            tournament = TournamentDEO.fromTournament(tournamentReport.tournament),
            code = tournamentReport.code.id.value,
            additionalInformation = tournamentReport.additionalInformation,
            isSubmitted = tournamentReport.isSubmitted,
            submitDate = tournamentReport.submitDate,
            selectedTeams = tournamentReport.selectedTeams.map {
                TeamDEO.fromTeam(it)
            },
            gameReports = tournamentReport.gameReports.map {
                CompleteGameReportDEO.fromGameReport(it)
            },
            pitches = tournamentReport.pitches.map {
                PitchDEO.fromPitch(it)
            },
            referee = RefereeDEO.fromReferee(tournamentReport.referee)
        )
        report
    }
    if (tournamentReport.isSubmitted) {
        launch { CacheUtil.cacheReport(report) }
    }
    return@coroutineScope report
}

fun TournamentReportByIdDEO.Companion.fromTournamentReport(tournamentReport: TournamentReport): TournamentReportByIdDEO {
    return transaction {
        TournamentReportByIdDEO(
            tournamentReport.id.value
        )
    }
}

suspend fun TournamentReportByIdDEO.submitInDatabase(): Result<TournamentReport> {
    val stdeo = this

    return suspendedTransactionAsync {
        val report = TournamentReport.findById(stdeo.id)
        if (report != null) {
            clearCacheForTournamentReport(report)

            report.isSubmitted = true
            report.submitDate = LocalDateTime.now()
            Result.success(report)
        } else {
            Result.failure(IllegalArgumentException("TournamentReport not found"))
        }
    }.await()
}

fun TournamentReportByIdDEO.createShareLink(): Result<TournamentReportShareLinkDEO> {
    val stdeo = this
    return transaction {
        val report = TournamentReport.findById(stdeo.id)
        if (report != null) {
            var share = TournamentReportShareLink.find { TournamentReportShareLinks.report eq report.id }.firstOrNull()
            if (share == null) {
                var uuid = UUID.randomUUID()
                while (TournamentReportShareLink.find { TournamentReportShareLinks.uuid eq uuid }
                        .firstOrNull() != null) {
                    uuid = UUID.randomUUID()
                }
                share = TournamentReportShareLink.new {
                    this.report = report
                    this.uuid = uuid
                }
            }
            Result.success(TournamentReportShareLinkDEO(report.id.value, share.uuid.toString()))

        } else {
            Result.failure(IllegalArgumentException("TournamentReport not found"))
        }
    }
}

fun UpdateReportAdditionalInformationDEO.Companion.fromTournamentReportReport(report: TournamentReport): UpdateReportAdditionalInformationDEO {
    return transaction {
        UpdateReportAdditionalInformationDEO(
            id = report.id.value,
            additionalInformation = report.additionalInformation
        )
    }
}

suspend fun UpdateReportAdditionalInformationDEO.updateInDatabase(): Result<TournamentReport> {
    val update = this
    if (update.additionalInformation.isNotBlank()) {
        return newSuspendedTransaction {
            val report = TournamentReport.findById(update.id)
            if (report != null) {
                clearCacheForTournamentReport(report)
                report.additionalInformation = update.additionalInformation
                Result.success(report)
            } else {
                Result.failure(
                    IllegalArgumentException("Report with id ${update.id} not found")
                )
            }
        }
    } else {
        return Result.failure(
            IllegalArgumentException("Id and additional information must be set")
        )
    }
}


fun NewTournamentReportDEO.Companion.fromTournamentReport(input: TournamentReport): NewTournamentReportDEO {
    return transaction {
        NewTournamentReportDEO(
            input.id.value,
            input.tournament.id.value,
            input.selectedTeams.map { it.id.value },
            input.code.id.value
        )
    }
}

fun NewTournamentReportDEO.storeInDatabase(user: User): TournamentReport? {
    return transaction {
        val reportTournament = Tournament.findById(tournament)
        val reportGameCode = GameCode.findById(gameCode)
        if (reportTournament != null && reportGameCode != null) {
            val report = TournamentReport.new {
                tournament = reportTournament
                code = reportGameCode
                referee = user
            }
            selectedTeams.forEach { team ->
                val dbTeam = Team.findById(team)
                if (dbTeam != null) {
                    TournamentReportTeamPreSelection.new {
                        this.report = report
                        this.team = dbTeam
                    }
                }
            }
            report
        } else {
            null
        }
    }
}

suspend fun NewTournamentReportDEO.updateInDatabase(): Result<TournamentReport> {
    val trUpdate = this
    if (trUpdate.id != null) {
        return newSuspendedTransaction {
            val report = TournamentReport.findById(trUpdate.id)
            val tournament = Tournament.findById(trUpdate.tournament)
            val gameCode = GameCode.findById(trUpdate.gameCode)
            if (report != null && tournament != null && gameCode != null) {
                clearCacheForTournamentReport(report)
                report.tournament = tournament
                report.code = gameCode
                val connections = TournamentReportTeamPreSelection.find {
                    (TournamentReportTeamPreSelections.report eq report.id)
                }

                // Add teams selection that cannot be found in the database
                trUpdate.selectedTeams.forEach { updateTeamId ->
                    val matches = connections.count { connection ->
                        connection.team.id.value == updateTeamId
                    }
                    if (matches == 0) {
                        val dbTeam = Team.findById(updateTeamId)
                        if (dbTeam != null) {
                            TournamentReportTeamPreSelection.new {
                                this.report = report
                                this.team = dbTeam
                            }
                        }
                    }
                }
                // Remove teams selection that are not in the update
                TournamentReportTeamPreSelection.find { TournamentReportTeamPreSelections.report eq trUpdate.id }
                    .forEach {
                        if (!trUpdate.selectedTeams.contains(it.team.id.value)) {
                            it.delete()
                        }
                    }
                Result.success(report)
            } else {
                Result.failure(
                    IllegalArgumentException("TournamentReport or Tournament or GameCode not found")
                )
            }
        }
    } else {
        return Result.failure(
            IllegalArgumentException("TournamentReport id not found")
        )
    }
}


fun TournamentReportDEO.Companion.fromTournamentReport(report: TournamentReport): TournamentReportDEO {
    return TournamentReportDEO(
        report.id.value,
        report.tournament.id.value,
        report.code.id.value,
        report.additionalInformation,
        report.isSubmitted,
        report.submitDate
    )
}


@Deprecated("Use NewTournamentReportDEO instead")
fun TournamentReportDEO.createInDatabase(referee: User): TournamentReport? {
    val rUpdate = this
    if (rUpdate.id == null && rUpdate.tournament != null && rUpdate.code != null) {
        val tournament = Tournament.findById(rUpdate.tournament)
        val code = GameCode.findById(rUpdate.code)
        if (tournament != null && code != null) {
            return transaction {
                TournamentReport.new {
                    this.tournament = tournament
                    this.referee = referee
                    this.code = code
                    rUpdate.additionalInformation?.let {
                        this.additionalInformation = it
                    }
                    rUpdate.isSubmitted?.let {
                        this.isSubmitted = it
                    }
                    rUpdate.submitDate?.let {
                        this.submitDate = it
                    }
                }
            }
        }
    }
    return null
}

suspend fun TournamentReportDEO.updateInDatabase(): TournamentReport? {
    val rUpdate = this
    if (rUpdate.id != null) {
        return newSuspendedTransaction {
            val report = TournamentReport.findById(rUpdate.id)
            if (report != null) {
                clearCacheForTournamentReport(report)
                rUpdate.tournament?.let {
                    Tournament.findById(it)
                }?.let {
                    report.tournament = it
                }
                rUpdate.code?.let {
                    GameCode.findById(it)
                }?.let {
                    report.code = it
                }

                rUpdate.additionalInformation?.let {
                    report.additionalInformation = it
                }
                rUpdate.isSubmitted?.let {
                    report.isSubmitted = it
                }
                rUpdate.submitDate?.let {
                    report.submitDate = it
                }
                report
            } else {
                null
            }
        }
    }
    return null
}


fun CompactTournamentReportDEO.Companion.all(): List<CompactTournamentReportDEO> {
    return transaction {


        exec(
            "SELECT\n" +
                    "    TournamentReports.id,\n" +
                    "    TournamentReports.tournament,\n" +
                    "    TournamentReports.code,\n" +
                    "    TournamentReports.is_submitted,\n" +
                    "    TournamentReports.submit_date,\n" +
                    "    TournamentReports.referee,\n" +
                    "    TournamentReports.additional_information,\n" +
                    "    U.first_name,\n" +
                    "    U.last_name,\n" +
                    "    (SELECT COUNT(*) FROM GameReports GR WHERE TournamentReports.id = GR.report_id) as \"num_game_reports\",\n" +
                    "    (SELECT COUNT(*) FROM TournamentReportTeamPreSelections TRTPS WHERE TournamentReports.id = TRTPS.report) as \"num_teams\"\n" +
                    "FROM TournamentReports\n" +
                    "         LEFT JOIN Users U on TournamentReports.referee = U.id\n" +
                    "\n" +
                    "GROUP BY TournamentReports.id;"
        ) {
            val returnList = mutableListOf<CompactTournamentReportDEO>()
            while (it.next()) {
                returnList.add(
                    CompactTournamentReportDEO(
                        it.getLong("id"),
                        it.getLong("tournament"),
                        it.getLong("code"),
                        it.getBoolean("is_submitted"),
                        it.getTimestamp("submit_date")?.toLocalDateTime(),
                        it.getLong("referee"),
                        it.getString("first_name") + " " + it.getString("last_name"),
                        it.getLong("num_game_reports"),
                        it.getLong("num_teams"), it.getString("additional_information")
                    )
                )
            }
            returnList.toList()
        } ?: emptyList()


    }


}

fun CompactTournamentReportDEO.Companion.fromTournamentReport(report: TournamentReport): CompactTournamentReportDEO {
    return transaction {
        val numTeams = TournamentReportTeamPreSelections
            .slice(TournamentReportTeamPreSelections.id.count())
            .select { TournamentReportTeamPreSelections.report eq report.id }
            .first()[TournamentReportTeamPreSelections.id.count()]
        CompactTournamentReportDEO(
            report.id.value,
            report.tournament.id.value,
            report.code.id.value,
            report.isSubmitted,
            report.submitDate,
            report.referee.id.value,
            report.referee.firstName + " " + report.referee.lastName,
            report.gameReports.count(),
            numTeams,
            report.additionalInformation
        )
    }
}


/**
 * This function deletes the tournament report from the database
 * it will not check if the user is allowed to delete the report
 */
suspend fun DeleteTournamentReportDEO.deleteFromDatabase(): Result<Boolean> {
    val id = this.id
    return newSuspendedTransaction {
        TournamentReport.findById(id)?.let { it ->
            clearCacheForTournamentReport(it)
            it.deleteComplete()
            Result.success(true)
        } ?: Result.failure(Exception("TournamentReport with id $id not found"))
    }
}

suspend fun DeleteTournamentReportDEO.deleteChecked(user: User): Result<Boolean> {
    val id = this.id
    val hasRights = transaction {
        TournamentReport.findById(id)?.let { it ->
            it.referee == user || user.role == UserRole.ADMIN
        } ?: false
    }
    if (hasRights) {
        return deleteFromDatabase()
    }
    return Result.failure(Exception("User has no rights to delete TournamentReport with id $id"))
}

