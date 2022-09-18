package eu.gaelicgames.referee.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class TeamDEO(
    val name: String, val id: Long, val isAmalgamation: Boolean, val amalgamationTeams: List<TeamDEO>?
) {
    companion object {
        fun fromTeam(input: Team, amalgamationTeams: List<TeamDEO>? = null): TeamDEO {
            return TeamDEO(input.name, input.id.value, input.isAmalgamation, amalgamationTeams)
        }

    }
}

@Serializable
data class NewTeamDEO(val name: String)

@Serializable
data class NewAmalgamationDEO(val name: String, val teams: List<TeamDEO>)

@Serializable
data class TournamentDEO(
    val id: Long,
    val name: String,
    val location: String,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate
) {
    companion object {
        fun fromTournament(input: Tournament): TournamentDEO {
            return TournamentDEO(
                input.id.value, input.name, input.location, input.date
            )
        }
    }
}


@Serializable
data class NewTournamentDEO(
    val name: String, val location: String, @Serializable(with = LocalDateSerializer::class) val date: LocalDate
)

object LocalDateSerializer : KSerializer<LocalDate> {
    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(DateTimeFormatter.ISO_DATE.format(value))
    }

}

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(decoder.decodeString()))
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(DateTimeFormatter.ISO_DATE_TIME.format(value))
    }

}

@Serializable
data class GameCodeDEO(
    val name: String, val id: Long
) {
    constructor(gameCode: GameCode) : this(gameCode.name, gameCode.id.value)

}

@Serializable
data class RuleDEO(
    val id: Long,
    val code: Long,
    val isCaution: Boolean,
    val isBlack: Boolean,
    val isRed: Boolean,
    val description: String
) {
    constructor(rule: Rule) : this(
        rule.id.value, rule.code.id.value, rule.isCaution, rule.isBlack, rule.isRed, rule.description
    )
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

data class SubmitTournamentDEO(
    val id:Long
) {
    companion object {
        fun fromTournamentReport(tournamentReport: TournamentReport) {
            return transaction {
                SubmitTournamentDEO(
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
enum class ApiErrorOptions {
    @SerialName("insertionFailed")
    INSERTION_FAILED,

    @SerialName("deleteFailed")
    DELETE_FAILED,

    @SerialName("notFound")
    NOT_FOUND,
}

@Serializable
data class ApiError(
    val error: ApiErrorOptions, val message: String
)

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
data class GameTypeDEO(
    val id: Long? = null,
    val name: String,
) {
    companion object {
        fun fromGameType(gameType: GameType): GameTypeDEO {
            return transaction {
                GameTypeDEO(
                    gameType.id.value,
                    gameType.name
                )
            }
        }
    }

    fun createInDatabase(): Result<GameType> {
        val gUpdate = this
        if (gUpdate.id == null && gUpdate.name.isNotBlank()) {
            return Result.success(transaction {
                GameType.new {
                    this.name = gUpdate.name
                }
            })
        }
        return Result.failure(
            IllegalArgumentException("GameType name not found or already has an id")
        )
    }

    fun updateInDatabase(): Result<GameType> {
        val gUpdate = this
        if (gUpdate.id != null) {
            return transaction {
                val gameType = GameType.findById(gUpdate.id)
                if (gameType != null) {
                    gameType.name = gUpdate.name
                    Result.success(gameType)
                } else {
                    Result.failure(
                        IllegalArgumentException("GameType not found")
                    )
                }
            }
        }
        return Result.failure(
            IllegalArgumentException("GameType id not found")
        )
    }
}