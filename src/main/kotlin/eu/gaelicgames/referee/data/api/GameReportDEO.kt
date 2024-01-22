package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.GGERefereeConfig
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime


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
    val generalNotes: String? = null,
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
                    report.gameType?.id?.value,
                    report.extraTime?.id?.value,
                    report.umpirePresentOnTime,
                    report.umpireNotes,
                    report.generalNotes
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
            grUpdate.teamBPoints != null
        ) {

            return transaction {


                val report = TournamentReport.findById(grUpdate.report)
                val teamA = Team.findById(grUpdate.teamA)
                val teamB = Team.findById(grUpdate.teamB)
                val gameType = grUpdate.gameType?.let {
                    GameType.findById(it)
                }
                val extraTime = grUpdate.extraTime?.let {
                    ExtraTimeOption.findById(it)
                }
                if (report != null && teamA != null && teamB != null) {
                    val reportCreated = GameReport.new {
                        this.report = report
                        this.teamA = teamA
                        this.teamB = teamB
                        this.startTime = grUpdate.startTime
                        this.teamAGoals = grUpdate.teamAGoals
                        this.teamBGoals = grUpdate.teamBGoals
                        this.teamAPoints = grUpdate.teamAPoints
                        this.teamBPoints = grUpdate.teamBPoints
                        gameType?.let {
                            this.gameType = it
                        }
                        extraTime?.let {
                            this.extraTime = it
                        }
                        grUpdate.umpirePresentOnTime?.let {
                            this.umpirePresentOnTime = it
                        }
                        grUpdate.umpireNotes?.let {
                            this.umpireNotes = it
                        }
                        grUpdate.generalNotes?.let {
                            this.generalNotes = it
                        }
                    }

                    Result.success(reportCreated)
                } else {

                    Result.failure(
                        IllegalArgumentException(
                            "Trying to insert a game report with either an invalid " +
                                    "Team A ${grUpdate.teamA} B ${grUpdate.teamB} or invalid report ${grUpdate.report}"
                        )
                    )
                }
            }

        }
        return Result.failure(
            IllegalArgumentException("Trying to insert a game report with missing fields")
        )
    }

    fun updateInDatabase(): Result<GameReport> {
        if (this.id != null) {
            return transaction {
                val grUpdate = this@GameReportDEO
                val originalGameReport = GameReport.findById(this@GameReportDEO.id)
                if (originalGameReport != null) {

                    /*
                    if (grUpdate.report != null) {
                        //Dont act here as moving to another report is unsupported
                    }*/
                    grUpdate.teamA?.let { teamA ->
                        Team.findById(teamA)?.let { team ->
                            if (team != originalGameReport.teamA) {
                                originalGameReport.teamADisciplinaryActions().map {
                                    it.team = team
                                }
                                originalGameReport.teamAInjuries().map {
                                    it.team = team
                                }
                            }
                            originalGameReport.teamA = team
                        }
                    }
                    grUpdate.teamB?.let { teamB ->
                        Team.findById(teamB)?.let { team ->
                            if (team != originalGameReport.teamB) {
                                originalGameReport.teamBDisciplinaryActions().map {
                                    it.team = team
                                }
                                originalGameReport.teamBInjuries().map {
                                    it.team = team
                                }
                            }
                            originalGameReport.teamB = team
                        }
                    }
                    grUpdate.teamAGoals?.let { goals ->
                        originalGameReport.teamAGoals = goals
                    }
                    grUpdate.teamBGoals?.let { goals ->
                        originalGameReport.teamBGoals = goals
                    }
                    grUpdate.teamAPoints?.let { points ->
                        originalGameReport.teamAPoints = points
                    }
                    grUpdate.teamBPoints?.let { points ->
                        originalGameReport.teamBPoints = points
                    }
                    grUpdate.startTime?.let { startTime ->
                        originalGameReport.startTime = startTime
                    }
                    grUpdate.extraTime?.let { extraTime ->
                        ExtraTimeOption.findById(extraTime)?.let { eto ->
                            originalGameReport.extraTime = eto
                        }
                    }
                    grUpdate.gameType?.let { gameType ->
                        GameType.findById(gameType)?.let { gt ->
                            originalGameReport.gameType = gt
                        }
                    }
                    grUpdate.umpirePresentOnTime?.let { present ->
                        originalGameReport.umpirePresentOnTime = present
                    }
                    grUpdate.umpireNotes?.let { notes ->
                        originalGameReport.umpireNotes = notes
                    }
                    grUpdate.generalNotes?.let { notes ->
                        originalGameReport.generalNotes = notes
                    }
                    Result.success(originalGameReport)
                } else {
                    Result.failure(
                        IllegalArgumentException(
                            "Trying to update a game report with an invalid id"
                        )
                    )
                }
            }
        } else {
            return Result.failure(
                IllegalArgumentException(
                    "Trying to update a game report with no id"
                )
            )
        }
    }
}
@Serializable
data class DeleteGameReportDEO(
    val id: Long? = null
) {
    fun deleteChecked(user: User):Result<Boolean> {
        val deleteId = this.id ?: -1
        return transaction {
            GameReport.findById(deleteId)?.let {
                if(it.report.referee.id == user.id) {
                    deleteFromDatabase()
                } else {
                    Result.failure(IllegalArgumentException("No rights - User is not the referee of this game"))
                }
            } ?: Result.failure(IllegalArgumentException("Game report does not exist"))
        }
    }
    fun deleteFromDatabase(): Result<Boolean> {
        val deleteId = this.id
        if (deleteId != null) {
            return transaction {
                val originalGameReport = GameReport.findById(deleteId)

                if (originalGameReport != null) {
                    DisciplinaryAction.find { DisciplinaryActions.game eq originalGameReport.id }.forEach {
                        it.delete()
                    }
                    Injury.find { Injuries.game eq originalGameReport.id }.forEach {
                        it.delete()
                    }
                    originalGameReport.delete()
                    Result.success(true)
                } else {
                    Result.failure(
                        IllegalArgumentException(
                            "Trying to delete a game report with an invalid id"
                        )
                    )
                }
            }
        } else {
            return Result.failure(
                IllegalArgumentException(
                    "Trying to delete a game report with no id"
                )
            )
        }
    }
}

@Serializable
data class GameReportClassesDEO(
    val extraTimeOptions: List<ExtraTimeOptionDEO>,
    val gameTypes: List<GameTypeDEO>,
) {
    companion object {
        fun load(): GameReportClassesDEO {
            return transaction {
                val etos = ExtraTimeOption.all().map {
                    ExtraTimeOptionDEO.fromExtraTimeOption(it)
                }
                val gts = GameType.all().map {
                    GameTypeDEO.fromGameType(it)
                }
                GameReportClassesDEO(etos, gts)
            }
        }
    }
}

@Serializable
data class ExtraTimeOptionDEO(
    val id: Long,
    val name: String
) {
    companion object {
        fun fromExtraTimeOption(extraTimeOption: ExtraTimeOption): ExtraTimeOptionDEO {
            return ExtraTimeOptionDEO(
                extraTimeOption.id.value,
                extraTimeOption.name
            )
        }
    }
}



@Serializable
data class DisciplinaryActionDEO(
    val id: Long? = null,
    val team: Long? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val number: Int? = null,
    val details: String? = null,
    val rule: Long? = null,
    val game: Long? = null,
) {
    companion object {
        fun fromDisciplinaryAction(disciplinaryAction: DisciplinaryAction): DisciplinaryActionDEO {
            return transaction {
                DisciplinaryActionDEO(
                    disciplinaryAction.id.value,
                    disciplinaryAction.team.id.value,
                    disciplinaryAction.firstName,
                    disciplinaryAction.lastName,
                    disciplinaryAction.number,
                    disciplinaryAction.details,
                    disciplinaryAction.rule.id.value,
                    disciplinaryAction.game.id.value
                )
            }
        }
    }

    fun createInDatabase(): Result<DisciplinaryAction> {
        val daUpdate = this
        if (daUpdate.team != null &&
            daUpdate.firstName != null &&
            daUpdate.lastName != null &&
            daUpdate.number != null &&
            daUpdate.details != null &&
            daUpdate.rule != null &&
            daUpdate.game != null
        ) {

            return transaction {
                val rule = Rule.findById(daUpdate.rule)
                val team = Team.findById(daUpdate.team)
                val game = GameReport.findById(daUpdate.game)
                if (rule != null && team != null && game != null) {
                    Result.success(DisciplinaryAction.new {
                        this.team = team
                        this.firstName = daUpdate.firstName
                        this.lastName = daUpdate.lastName
                        this.number = daUpdate.number
                        this.details = daUpdate.details
                        this.rule = rule
                        this.game = game
                    })
                } else {
                    Result.failure(
                        IllegalArgumentException(
                            "Trying to insert a disciplinary action with either an invalid " +
                                    "Team $team or invalid rule $rule or invalid game $game"
                        )
                    )
                }
            }
        } else {
            return Result.failure(
                IllegalArgumentException(
                    "Trying to insert a disciplinary action with missing fields"
                )
            )
        }
    }

    fun updateInDatabase(): Result<DisciplinaryAction> {
        val daUpdate = this

        if (daUpdate.id != null) {
            return transaction {
                val action = DisciplinaryAction.findById(daUpdate.id)
                if (action != null) {
                    daUpdate.firstName?.let { firstName ->
                        action.firstName = firstName
                    }
                    daUpdate.lastName?.let { lastName ->
                        action.lastName = lastName
                    }
                    daUpdate.number?.let { number ->
                        action.number = number
                    }
                    daUpdate.details?.let { details ->
                        action.details = details
                    }
                    daUpdate.team?.let { team ->
                        Team.findById(team)?.let {
                            action.team = it
                        }
                    }
                    daUpdate.rule?.let { rule ->
                        Rule.findById(rule)?.let { r ->
                            action.rule = r
                        }
                    }
                    Result.success(action)
                } else {
                    Result.failure(
                        IllegalArgumentException(
                            "Trying to update a disciplinary action with invalid id ${daUpdate.id}"
                        )
                    )
                }

            }
        } else {
            return Result.failure(
                IllegalArgumentException(
                    "Trying to update a disciplinary action with missing id"
                )
            )
        }
    }
}


data class DisciplinaryActionStringDEO(
    val id: Long,
    val teamName: String,
    val opposingTeamName: String,
    val firstName: String,
    val lastName: String,
    val number: Int,
    val details: String,
    val ruleName: String,
    val tournamentName: String,
    val tournamentLocation: String,
    val tournamentDateTime: LocalDateTime,
    val reportShareLink: String
) {
    companion object {
        fun fromDisciplinaryAction(disciplinaryAction: DisciplinaryAction): DisciplinaryActionStringDEO {
            return transaction {

                val opposingTeam = setOf(
                    disciplinaryAction.game.teamA, disciplinaryAction.game.teamB
                ).first {
                    it != disciplinaryAction.team
                }

                val reportShareLink = TournamentReportByIdDEO.fromTournamentReport(
                    disciplinaryAction.game.report
                ).createShareLink()
                    .map { GGERefereeConfig.serverUrl + "/report/share/" + it.uuid }
                    .getOrElse { GGERefereeConfig.serverUrl }
                DisciplinaryActionStringDEO(
                    disciplinaryAction.id.value,
                    disciplinaryAction.team.name,
                    opposingTeam.name,
                    disciplinaryAction.firstName,
                    disciplinaryAction.lastName,
                    disciplinaryAction.number,
                    disciplinaryAction.details,
                    disciplinaryAction.rule.description,
                    disciplinaryAction.game.report.tournament.name,
                    disciplinaryAction.game.report.tournament.location,
                    disciplinaryAction.game.startTime ?: LocalDateTime.now(),
                    reportShareLink
                )
            }
        }
    }
}

@Serializable
data class DeleteDisciplinaryActionDEO(
    val id: Long
) {
    fun deleteChecked(user: User):Result<Boolean> {
        val deleteId = this.id
        return transaction {
            DisciplinaryAction.findById(deleteId)?.let {
                if(it.game.report.referee.id == user.id) {
                    deleteFromDatabase()
                } else {
                    Result.failure(IllegalArgumentException("No rights - User is not the referee of this game"))
                }
            } ?: Result.failure(IllegalArgumentException("Disciplinary Action does not exist"))
        }
    }
    fun deleteFromDatabase(): Result<Boolean> {
        val deleteId = this.id
        return transaction {
            val action = DisciplinaryAction.findById(deleteId)
            if (action != null) {
                action.delete()
                Result.success(true)
            } else {
                Result.failure(
                    IllegalArgumentException(
                        "Trying to delete a disciplinary action with invalid id ${this.id}"
                    )
                )
            }
        }
    }
}
@Serializable
data class InjuryDEO(
    val id: Long? = null,
    val team: Long? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val details: String? = null,
    val game: Long? = null,
) {
    companion object {
        fun fromInjury(injury: Injury): InjuryDEO {
            return transaction {
                InjuryDEO(
                    injury.id.value,
                    injury.team.id.value,
                    injury.firstName,
                    injury.lastName,
                    injury.details,
                    injury.game.id.value
                )
            }
        }
    }

    fun createInDatabase(): Result<Injury> {
        val injuryUpdate = this
        if (injuryUpdate.team != null &&
            injuryUpdate.firstName != null &&
            injuryUpdate.lastName != null &&
            injuryUpdate.details != null &&
            injuryUpdate.game != null
        ) {

            return transaction {
                val team = Team.findById(injuryUpdate.team)
                val game = GameReport.findById(injuryUpdate.game)
                if (team != null && game != null) {
                    Result.success(Injury.new {
                        this.team = team
                        this.firstName = injuryUpdate.firstName
                        this.lastName = injuryUpdate.lastName
                        this.details = injuryUpdate.details
                        this.game = game
                    })
                } else {
                    Result.failure(
                        IllegalArgumentException(
                            "Trying to insert an injury with either an invalid " +
                                    "Team $team or invalid game $game"
                        )
                    )
                }
            }
        } else {
            return Result.failure(
                IllegalArgumentException(
                    "Trying to insert an injury with missing fields"
                )
            )
        }
    }

    fun updateInDatabase(): Result<Injury> {
        val injuryUpdate = this
        if (injuryUpdate.id != null) {
            return transaction {
                val injury = Injury.findById(injuryUpdate.id)
                if (injury != null) {
                    injuryUpdate.firstName?.let { firstName ->
                        injury.firstName = firstName
                    }
                    injuryUpdate.lastName?.let { lastName ->
                        injury.lastName = lastName
                    }
                    injuryUpdate.details?.let { details ->
                        injury.details = details
                    }
                    injuryUpdate.team?.let { team ->
                        Team.findById(team)?.let {
                            injury.team = it
                        }
                    }
                    injuryUpdate.game?.let { game ->
                        GameReport.findById(game)?.let { g ->
                            injury.game = g
                        }
                    }
                    Result.success(injury)
                } else {
                    Result.failure(
                        IllegalArgumentException(
                            "Trying to update an injury with invalid id ${injuryUpdate.id}"
                        )
                    )
                }
            }
        } else {
            return Result.failure(
                IllegalArgumentException(
                    "Trying to update an injury with missing id"
                )
            )
        }
    }
}

@Serializable
data class DeleteInjuryDEO(
    val id:Long
) {
    fun deleteChecked(user: User):Result<Boolean> {
        val deleteId = this.id
        return transaction {
            Injury.findById(deleteId)?.let {
                if(it.game.report.referee.id == user.id) {
                    deleteFromDatabase()
                } else {
                    Result.failure(IllegalArgumentException("No rights - User is not the referee of this game"))
                }
            } ?: Result.failure(IllegalArgumentException("Injury does not exist"))
        }
    }
    fun deleteFromDatabase(): Result<Boolean> {
        val deleteId = this.id
        return transaction {
            val injury = Injury.findById(deleteId)
            if (injury != null) {
                injury.delete()
                Result.success(true)
            } else {
                Result.failure(
                    IllegalArgumentException(
                        "Trying to delete an injury with invalid id ${this.id}"
                    )
                )
            }
        }
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

@Serializable
data class CompleteGameReportDEO(
    val gameReport: GameReportDEO,
    val injuries: List<InjuryDEO>,
    val disciplinaryActions: List<DisciplinaryActionDEO>,
) {
    companion object {
        fun fromGameReport(gameReport: GameReport): CompleteGameReportDEO {
            return transaction {
                CompleteGameReportDEO(
                    gameReport = GameReportDEO.fromGameReport(gameReport),
                    injuries = gameReport.injuries.map { InjuryDEO.fromInjury(it) },
                    disciplinaryActions = gameReport.disciplinaryActions.map {
                        DisciplinaryActionDEO.fromDisciplinaryAction(
                            it
                        )
                    })
            }
        }
    }
}

@Serializable
data class CompleteGameReportWithRefereeReportDEO(
    val gameReport: CompleteGameReportDEO,
    val refereeReport: CompactTournamentReportDEO
) {
    companion object {
        fun fromGameReport(gameReport: GameReport): CompleteGameReportWithRefereeReportDEO {
            return transaction {
                CompleteGameReportWithRefereeReportDEO(
                    gameReport = CompleteGameReportDEO.fromGameReport(gameReport),
                    refereeReport = CompactTournamentReportDEO.fromTournamentReport(
                        gameReport.report
                    )
                )
            }
        }
    }
}

@Serializable
data class PublicDisciplinaryActionDEO(
    val id: Long? = null,
    val team: Long? = null,
    val rule: Long? = null,
    val game: Long? = null,
) {
    companion object{
        fun fromDisciplinaryAction(disciplinaryAction: DisciplinaryAction): PublicDisciplinaryActionDEO {
            return transaction {
                PublicDisciplinaryActionDEO(
                    id = disciplinaryAction.id.value,
                    team = disciplinaryAction.team.id.value,
                    rule = disciplinaryAction.rule.id.value,
                    game = disciplinaryAction.game.id.value
                )
            }
        }
    }
}

@Serializable
data class PublicGameReportDEO(
    val gameReport: GameReportDEO,
    val disciplinaryActions: List<PublicDisciplinaryActionDEO>,
    val code: Long,
) {
    companion object {
        fun fromGameReport(gameReport: GameReport): PublicGameReportDEO {
            return transaction {
                PublicGameReportDEO(
                    gameReport = GameReportDEO.fromGameReport(gameReport),
                    disciplinaryActions = gameReport.disciplinaryActions.map {
                        PublicDisciplinaryActionDEO.fromDisciplinaryAction(
                            it
                        )
                    },
                    code = gameReport.report.code.id.value
                )


            }
        }
    }
}
