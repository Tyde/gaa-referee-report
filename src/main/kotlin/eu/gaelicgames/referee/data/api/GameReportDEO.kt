package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
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
data class GameReportClasses(
    val extraTimeOptions: List<ExtraTimeOptionDAO>,
    val gameTypes: List<GameTypeDAO>,
) {
    companion object {
        fun load(): GameReportClasses {
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
