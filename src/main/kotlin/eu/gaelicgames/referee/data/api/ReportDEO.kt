package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
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
        if(update.additionalInformation.isNotBlank()) {
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

@Serializable
data class CompactTournamentReportDEO(
    val id:Long,
    val tournament: Long,
    val code: Long,
    val isSubmitted: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class) val submitDate: LocalDateTime? = null,
    val refereeId: Long,
    val refereeName: String,
    val numGameReports: Long,
    val numTeams: Long,
) {
    companion object {
        fun all(): List<CompactTournamentReportDEO> {
            return transaction {


                exec("SELECT\n" +
                        "    TournamentReports.id,\n" +
                        "    TournamentReports.tournament,\n" +
                        "    TournamentReports.code,\n" +
                        "    TournamentReports.is_submitted,\n" +
                        "    TournamentReports.submit_date,\n" +
                        "    TournamentReports.referee,\n" +
                        "    U.first_name,\n" +
                        "    U.last_name,\n" +
                        "    (SELECT COUNT(*) FROM GameReports GR WHERE TournamentReports.id = GR.report_id) as \"num_game_reports\",\n" +
                        "    (SELECT COUNT(*) FROM TournamentReportTeamPreSelections TRTPS WHERE TournamentReports.id = TRTPS.report) as \"num_teams\"\n" +
                        "FROM TournamentReports\n" +
                        "         LEFT JOIN Users U on TournamentReports.referee = U.id\n" +
                        "\n" +
                        "GROUP BY TournamentReports.id;") {
                    val returnList = mutableListOf<CompactTournamentReportDEO>()
                    while(it.next()) {
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
                            it.getLong("num_teams")
                        ))
                    }
                    returnList.toList()
                }?: emptyList()
                /*

                  val numGameReportsAlias = GameReports.id.count().alias("numGameReports")
                val subQueryGR = GameReports.slice(numGameReportsAlias)
                    .select { GameReports.report eq TournamentReports.id }
                val numTeamsAlias = TournamentReportTeamPreSelections.id.count().alias("numTeams")
                val subQueryTRT = TournamentReportTeamPreSelections
                    .slice(numTeamsAlias)
                    .select { TournamentReportTeamPreSelections.report eq TournamentReports.id }.alias("subQueryTRT")
                (TournamentReports
                        leftJoin Users).slice(
                    TournamentReports.id,
                    TournamentReports.tournament,
                    TournamentReports.code,
                    TournamentReports.isSubmitted,
                    TournamentReports.submitDate,
                    Users.id,
                    Users.firstName,
                    Users.lastName,
                    subQueryGR.,
                    subQueryTRT[numTeamsAlias]
                        ).selectAll().groupBy(TournamentReports.id).map { row->
                    CompactTournamentReportDEO(
                        row[TournamentReports.id].value,
                        row[TournamentReports.tournament].value,
                        row[TournamentReports.code].value,
                        row[TournamentReports.isSubmitted],
                        row[TournamentReports.submitDate],
                        row[Users.id].value,
                        row[Users.firstName]+" "+row[Users.lastName],
                        row[GameReports.id.count()],
                        row[TournamentReportTeamPreSelections.id.count()]
                    )

                }.toList()*/


            }


        }
        fun fromTournamentReport(report: TournamentReport): CompactTournamentReportDEO {
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
                    numTeams
                )
            }
        }
    }
}

@Serializable
data class DeleteTournamentReportDEO(
    val id: Long
) {


    /**
     * This function deletes the tournament report from the database
     * it will not check if the user is allowed to delete the report
     */
    fun deleteFromDatabase(): Result<Boolean> {
        val id = this.id
        return transaction {
            TournamentReport.findById(id)?.let{ it->
                it.deleteComplete()
                Result.success(true)
            } ?: Result.failure(Exception("TournamentReport with id $id not found"))
        }
    }

    fun deleteChecked(user: User) :Result<Boolean> {
        val id = this.id
        val hasRights =  transaction {
            TournamentReport.findById(id)?.let{ it->
                it.referee == user || user.role == UserRole.ADMIN
            } ?: false
        }
        if(hasRights) {
            return deleteFromDatabase()
        }
        return Result.failure(Exception("User has no rights to delete TournamentReport with id $id"))
    }
}