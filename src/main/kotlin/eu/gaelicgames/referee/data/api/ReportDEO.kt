package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

@Serializable
data class CompleteReportDEO(
    val id: Long,
    val tournament: TournamentDEO,
    val code: Long,
    val additionalInformation: String,
    val isSubmitted: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class) val submitDate: LocalDateTime?,
    val selectedTeams: List<TeamDEO>,
    val gameReports: List<CompleteGameReportDEO>,
    val pitches: List<PitchDEO>,
    val referee: RefereeDEO,
) {
    companion object {
        fun fromTournamentReport(tournamentReport: TournamentReport): CompleteReportDEO {
            return transaction {
                CompleteReportDEO(
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
            }
        }
    }
}

@Serializable
data class SubmitTournamentReportDEO(
    val id:Long
) {
    companion object {
        fun fromTournamentReport(tournamentReport: TournamentReport): SubmitTournamentReportDEO {
            return transaction {
                SubmitTournamentReportDEO(
                    tournamentReport.id.value
                )
            }
        }
    }
    fun submitInDatabase():Result<TournamentReport> {
        val stdeo = this
        return transaction {
            val report = TournamentReport.findById(stdeo.id)
            if (report != null) {
                report.isSubmitted = true
                report.submitDate = LocalDateTime.now()
                Result.success(report)
            } else {
                Result.failure(IllegalArgumentException("TournamentReport not found"))
            }
        }
    }
}
@Serializable
data class UpdateReportAdditionalInformationDEO(
    val id: Long,
    val additionalInformation: String
) {
    companion object {
        fun fromTournamentReportReport(report: TournamentReport): UpdateReportAdditionalInformationDEO {
            return transaction {
                UpdateReportAdditionalInformationDEO(
                    id = report.id.value,
                    additionalInformation = report.additionalInformation
                )
            }
        }
    }

    fun updateInDatabase(): Result<TournamentReport> {
        val update = this
        if(update.id != null && update.additionalInformation.isNotBlank()) {
            return transaction {
                val report = TournamentReport.findById(update.id)
                if (report != null) {
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
}

@Serializable
data class NewTournamentReportDEO(
    val id: Long? = null,
    val tournament: Long,
    val selectedTeams: List<Long>,
    val gameCode: Long
) {
    companion object {
        fun fromTournamentReport(input: TournamentReport): NewTournamentReportDEO {
            return transaction {
                NewTournamentReportDEO(
                    input.id.value,
                    input.tournament.id.value,
                    input.selectedTeams.map { it.id.value },
                    input.code.id.value
                )
            }
        }
    }

    fun storeInDatabase(user: User): TournamentReport? {
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

    fun updateInDatabase(): Result<TournamentReport> {
        val trUpdate = this
        if (trUpdate.id != null) {
            return transaction {
                val report = TournamentReport.findById(trUpdate.id)
                val tournament = Tournament.findById(trUpdate.tournament)
                val gameCode = GameCode.findById(trUpdate.gameCode)
                if (report != null && tournament != null && gameCode != null) {
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
}

@Serializable
data class TournamentReportDEO(
    val id: Long? = null,
    val tournament: Long? = null,
    val code: Long? = null,
    val additionalInformation: String? = null,
    val isSubmitted: Boolean? = null,
    @Serializable(with = LocalDateTimeSerializer::class) val submitDate: LocalDateTime? = null,
) {

    companion object {
        fun fromTournamentReport(report: TournamentReport): TournamentReportDEO {
            return TournamentReportDEO(
                report.id.value,
                report.tournament.id.value,
                report.code.id.value,
                report.additionalInformation,
                report.isSubmitted,
                report.submitDate
            )
        }
    }

    @Deprecated("Use NewTournamentReportDEO instead")
    fun createInDatabase(referee: User): TournamentReport? {
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

    fun updateInDatabase(): TournamentReport? {
        val rUpdate = this
        if (rUpdate.id != null) {
            return transaction {
                val report = TournamentReport.findById(rUpdate.id)
                if (report != null) {
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


}