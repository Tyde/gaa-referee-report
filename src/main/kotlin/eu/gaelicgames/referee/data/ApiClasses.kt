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
    val name: String,
    val id: Long,
    val isAmalgamation: Boolean,
    val amalgamationTeams: List<TeamDEO>?
) {
    companion object {
        fun createFromTeam(input: Team, amalgamationTeams: List<TeamDEO>? = null): TeamDEO {
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
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate
) {
    companion object {
        fun createFromTournament(input: Tournament): TournamentDEO {
            return TournamentDEO(
                input.id.value,
                input.name,
                input.location,
                input.date
            )
        }
    }
}


@Serializable
data class NewTournamentDEO(
    val name: String,
    val location: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate
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
    val name: String,
    val id: Long
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
        rule.id.value,
        rule.code.id.value,
        rule.isCaution,
        rule.isBlack,
        rule.isRed,
        rule.description
    )
}

@Serializable
data class NewTournamentReportDEO(
    val tournament: Long,
    val selectedTeams: List<Long>,
    val gameCode: Long
) {
    fun storeInDatabase(user: User): TournamentReport? {
        return transaction {
            val reportTournament = Tournament.findById(tournament.toLong())
            val reportGameCode = GameCode.findById(gameCode)
            if (reportTournament != null &&
                reportGameCode != null
            ) {
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
}

@Serializable
enum class UpdateCommand {
    @SerialName("updateGameReport")
    UPDATE_GAME_REPORT,
    @SerialName("updateTournamentReport")
    UPDATE_TOURNAMENT_REPORT,
}

@Serializable
data class ReportUpdate(
    val command: UpdateCommand,
    val report: TournamentReportDEO,
    val gameReport: GameReportDEO
)

@Serializable
enum class UpdateErrorOptions {
    @SerialName("insertionFailed")
    INSERTION_FAILED,
}

@Serializable
data class UpdateError(
    val error: UpdateErrorOptions,
    val message: String
)

@Serializable
data class TournamentReportDEO(
    val id: Long? = null,
    val tournament: Long? = null,
    val code: Long? = null,
    val additionalInformation: String? = null,
    val isSubmitted: Boolean? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val submitDate: LocalDateTime? = null,
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
        if (rUpdate.id == null &&
            rUpdate.tournament != null &&
            rUpdate.code != null
        ) {
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
data class GameReportDEO(
    val id: Long? = null,
    val report: Long? = null,
    val teamA: Long? = null,
    val teamB: Long? = null,
    val teamAGoals: Int? = null,
    val teamBGoals: Int? = null,
    val teamAPoints: Int? = null,
    val teamBPoints: Int? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val startTime: LocalDateTime? = null,
    val gameType: Long? = null,
    val extraTime: Long? = null,
    val umpirePresentOnTime: Boolean? = null,
    val umpireNotes: String? = null,
) {
    companion object {
        fun fromGameReport(report: GameReport): GameReportDEO {
            return transaction {
                 GameReportDEO(
                    report.id.value,
                    report.report.id.value,
                    report.teamA.id.value,
                    report.teamB.id.value,
                    report.teamAGoals,
                    report.teamBGoals,
                    report.teamAPoints,
                    report.teamBPoints,
                    report.startTime,
                    report.gameType.id.value,
                    report.extraTime?.id?.value,
                    report.umpirePresentOnTime,
                    report.umpireNotes
                )
            }
        }
    }

    fun createInDatabase(): Result<GameReport> {
        val grUpdate = this
        if (grUpdate.id == null &&
            grUpdate.report != null &&
            grUpdate.teamA != null &&
            grUpdate.teamB != null &&
            grUpdate.startTime != null &&
            grUpdate.teamAGoals != null &&
            grUpdate.teamBGoals != null &&
            grUpdate.teamAPoints != null &&
            grUpdate.teamBPoints != null &&
            grUpdate.gameType != null
        ) {

            return transaction {


                val report = TournamentReport.findById(grUpdate.report)
                val teamA = Team.findById(grUpdate.teamA)
                val teamB = Team.findById(grUpdate.teamB)
                val gameType = GameType.findById(grUpdate.gameType)
                val extraTime = grUpdate.extraTime?.let {
                    ExtraTimeOption.findById(it)
                }
                if (report != null && teamA != null && teamB != null && gameType != null) {
                    val reportCreated = GameReport.new {
                        this.report = report
                        this.teamA = teamA
                        this.teamB = teamB
                        this.startTime = grUpdate.startTime
                        this.teamAGoals = grUpdate.teamAGoals
                        this.teamBGoals = grUpdate.teamBGoals
                        this.teamAPoints = grUpdate.teamAPoints
                        this.teamBPoints = grUpdate.teamBPoints
                        this.gameType = gameType
                        extraTime?.let {
                            this.extraTime = it
                        }
                        grUpdate.umpirePresentOnTime?.let {
                            this.umpirePresentOnTime = it
                        }
                        grUpdate.umpireNotes?.let {
                            this.umpireNotes = it
                        }
                    }

                    Result.success(reportCreated)
                } else {

                    Result.failure(
                        IllegalArgumentException(
                            "Trying to insert a game report with either an invalid " +
                                    "Team A $(teamA) B $(teamB) or invalid report $(report) or" +
                                    "invalid game type $(gameType)"
                        )
                    )
                }
            }
        }
        return Result.failure(
            IllegalArgumentException("Trying to insert a game report with missing fields")
        )
    }

    fun updateInDatabase(): GameReport? {
        if (this.id != null) {
            return transaction {
                val grUpdate = this@GameReportDEO
                GameReport.findById(this@GameReportDEO.id)?.let {
                    if (grUpdate.report != null) {
                        //Dont act here as moving to another report is unsupported
                    }
                    grUpdate.teamA?.let { teamA ->
                        Team.findById(teamA)?.let { team ->
                            it.teamA = team
                        }
                    }
                    grUpdate.teamB?.let { teamB ->
                        Team.findById(teamB)?.let { team ->
                            it.teamB = team
                        }
                    }
                    grUpdate.teamAGoals?.let { goals ->
                        it.teamAGoals = goals
                    }
                    grUpdate.teamBGoals?.let { goals ->
                        it.teamBGoals = goals
                    }
                    grUpdate.teamAPoints?.let { points ->
                        it.teamAPoints = points
                    }
                    grUpdate.teamBPoints?.let { points ->
                        it.teamBPoints = points
                    }
                    grUpdate.startTime?.let { startTime ->
                        it.startTime = startTime
                    }
                    grUpdate.extraTime?.let { extraTime ->
                        ExtraTimeOption.findById(extraTime)?.let { eto ->
                            it.extraTime = eto
                        }
                    }
                    grUpdate.umpirePresentOnTime?.let { present ->
                        it.umpirePresentOnTime = present
                    }
                    grUpdate.umpireNotes?.let { notes ->
                        it.umpireNotes = notes
                    }
                    it
                }
            }
        }
        return null
    }
}

@Serializable
data class GameReportClasses(
    val extraTimeOptions: List<ExtraTimeOptionDAO>,
    val gameTypes: List<GameTypeDAO>,
) {
    companion object {
        fun load():GameReportClasses {
            return transaction {
                val etos = ExtraTimeOption.all().map {
                    ExtraTimeOptionDAO.fromExtraTimeOption(it)
                }
                val gts = GameType.all().map {
                    GameTypeDAO.fromGameType(it)
                }
                GameReportClasses(etos, gts)
            }
        }
    }
}

@Serializable
data class ExtraTimeOptionDAO(
    val id: Long,
    val name: String
) {
    companion object {
        fun fromExtraTimeOption(extraTimeOption: ExtraTimeOption): ExtraTimeOptionDAO {
            return ExtraTimeOptionDAO(
                extraTimeOption.id.value,
                extraTimeOption.name
            )
        }
    }
}

@Serializable
data class GameTypeDAO(
    val id: Long,
    val name: String
) {
    companion object {
        fun fromGameType(gameType: GameType): GameTypeDAO {
            return GameTypeDAO(
                gameType.id.value,
                gameType.name
            )
        }
    }
}