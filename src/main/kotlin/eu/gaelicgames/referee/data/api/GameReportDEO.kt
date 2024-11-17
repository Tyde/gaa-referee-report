package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.CacheUtil.deleteCache
import eu.gaelicgames.referee.util.CacheUtil.getCache
import eu.gaelicgames.referee.util.CacheUtil.setCache
import eu.gaelicgames.referee.util.GGERefereeConfig
import eu.gaelicgames.referee.util.lockedTransaction
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

val logger = LoggerFactory.getLogger(GameReportDEO::class.java)

suspend fun GameReportDEO.Companion.fromGameReport(report: GameReport): GameReportDEO {
    return lockedTransaction {
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


suspend fun GameReportDEO.Companion.wrapRow(row: ResultRow): GameReportDEO {
    return GameReportDEO(
        row[GameReports.id].value,
        row[GameReports.report].value,
        row[GameReports.teamA].value,
        row[GameReports.teamB].value,
        row[GameReports.teamAGoals],
        row[GameReports.teamBGoals],
        row[GameReports.teamAPoints],
        row[GameReports.teamBPoints],
        row[GameReports.startTime],
        row[GameReports.gameType]?.value,
        row[GameReports.extraTime]?.value,
        row[GameReports.umpirePresentOnTime],
        row[GameReports.umpireNotes],
        row[GameReports.generalNotes]
    )


}

suspend fun GameReportDEO.getRefereeId(): Long? {
    return lockedTransaction {
        this@getRefereeId.report?.let {
            TournamentReport.findById(it)?.referee?.id?.value
        }
    }

}

suspend fun GameReportDEO.createInDatabase(): Result<GameReport> {
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

        CacheUtil.deleteCachedReport(grUpdate.report)

        return lockedTransaction {
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
                val tournamentID = report.tournament.id.value
                runBlocking {
                    CacheUtil.deleteCachedCompleteTournamentReport(tournamentID)
                    CacheUtil.deleteCachedPublicTournamentReport(tournamentID)
                }
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

suspend fun GameReportDEO.updateInDatabase(): Result<GameReport> {

    if (this.id != null) {
        return lockedTransaction {
            val grUpdate = this@updateInDatabase
            val originalGameReport = GameReport.findById(this@updateInDatabase.id)
            if (originalGameReport != null) {
                clearCacheForGameReport(originalGameReport)

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


suspend fun DeleteGameReportDEO.deleteChecked(user: User): Result<Boolean> {
    val deleteId = this.id ?: -1
    return lockedTransaction {
        GameReport.findById(deleteId)?.let {

            if (it.report.referee.id == user.id) {
                deleteFromDatabase()
            } else {
                Result.failure(IllegalArgumentException("No rights - User is not the referee of this game"))
            }
        } ?: Result.failure(IllegalArgumentException("Game report does not exist"))
    }
}

suspend fun DeleteGameReportDEO.deleteFromDatabase(): Result<Boolean> {
    val deleteId = this.id
    if (deleteId != null) {
        return lockedTransaction {
            val originalGameReport = GameReport.findById(deleteId)

            if (originalGameReport != null) {
                clearCacheForGameReport(originalGameReport)

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


suspend fun GameReportClassesDEO.Companion.load(): GameReportClassesDEO {

    logger.debug("Loading GameReportClassesDEO from cache")
    val cachedData = GameReportClassesDEO.getCache()
    logger.debug("Cache hit success: ${cachedData.isSuccess}")
    return cachedData.getOrElse {
        lockedTransaction {
            val etos = ExtraTimeOption.all().map {
                ExtraTimeOptionDEO.fromExtraTimeOption(it)
            }
            val gts = GameType.all().map {
                GameTypeDEO.fromGameType(it)
            }
            val dbgrc = GameReportClassesDEO(etos, gts)
            runBlocking { dbgrc.setCache() }
            dbgrc
        }
    }

}

fun ExtraTimeOptionDEO.Companion.fromExtraTimeOption(extraTimeOption: ExtraTimeOption): ExtraTimeOptionDEO {
    return ExtraTimeOptionDEO(
        extraTimeOption.id.value,
        extraTimeOption.name
    )
}


suspend fun DisciplinaryActionDEO.Companion.fromDisciplinaryAction(disciplinaryAction: DisciplinaryAction): DisciplinaryActionDEO {
    return lockedTransaction {
        DisciplinaryActionDEO(
            disciplinaryAction.id.value,
            disciplinaryAction.team.id.value,
            disciplinaryAction.firstName,
            disciplinaryAction.lastName,
            disciplinaryAction.number,
            disciplinaryAction.details,
            disciplinaryAction.rule.id.value,
            disciplinaryAction.game.id.value,
            disciplinaryAction.redCardIssued
        )
    }
}

suspend fun DisciplinaryActionDEO.Companion.wrapRow(row: ResultRow): DisciplinaryActionDEO {
    return lockedTransaction {
        DisciplinaryActionDEO(
            row[DisciplinaryActions.id].value,
            row[DisciplinaryActions.team].value,
            row[DisciplinaryActions.firstName],
            row[DisciplinaryActions.lastName],
            row[DisciplinaryActions.number],
            row[DisciplinaryActions.details],
            row[DisciplinaryActions.rule].value,
            row[DisciplinaryActions.game].value,
            row[DisciplinaryActions.redCardIssued]
        )
    }

}

suspend fun DisciplinaryActionDEO.getRefereeId(): Long? {
    return lockedTransaction {
        TournamentReports.leftJoin(GameReports)
            .selectAll()
            .where { GameReports.id eq this@getRefereeId.game }
            .firstOrNull()
            ?.let {
                it[TournamentReports.referee].value
            }
    }

}

suspend fun DisciplinaryActionDEO.createInDatabase(): Result<DisciplinaryAction> {
    val daUpdate = this
    if (daUpdate.team != null &&
        daUpdate.firstName != null &&
        daUpdate.lastName != null &&
        daUpdate.number != null &&
        daUpdate.details != null &&
        daUpdate.rule != null &&
        daUpdate.game != null &&
        (daUpdate.firstName + daUpdate.lastName).isNotBlank()
    ) {

        return lockedTransaction {
            val rule = Rule.findById(daUpdate.rule)
            val team = Team.findById(daUpdate.team)
            val game = GameReport.findById(daUpdate.game)

            if (rule != null && team != null && game != null) {
                clearCacheForGameReport(game)

                Result.success(DisciplinaryAction.new {
                    this.team = team
                    this.firstName = daUpdate.firstName
                    this.lastName = daUpdate.lastName
                    this.number = daUpdate.number
                    this.details = daUpdate.details
                    this.rule = rule
                    this.game = game
                    this.redCardIssued = daUpdate.redCardIssued ?: false
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

suspend fun DisciplinaryActionDEO.updateInDatabase(): Result<DisciplinaryAction> {
    val daUpdate = this

    if (daUpdate.id != null) {
        println("Start update for ${daUpdate.id}")
        return lockedTransaction {
            val action = DisciplinaryAction.findById(daUpdate.id)
            if (action != null) {
                val game = action.game

                clearCacheForGameReport(game)
                daUpdate.firstName?.let { firstName ->
                    if (firstName.isNotBlank()) {
                        action.firstName = firstName
                    }
                }

                daUpdate.lastName?.let { lastName ->
                    if (lastName.isNotBlank()) {
                        action.lastName = lastName
                    }
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
                daUpdate.redCardIssued?.let { redCardIssued ->
                    action.redCardIssued = redCardIssued
                }
                println("Finish update for ${daUpdate.id}")
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


suspend fun DisciplinaryActionStringDEO.Companion.fromDisciplinaryAction(disciplinaryAction: DisciplinaryAction): DisciplinaryActionStringDEO {
    return lockedTransaction {

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
            reportShareLink,
            disciplinaryAction.redCardIssued
        )
    }
}

suspend fun DisciplinaryActionStringDEO.Companion.getRedCardsIssuedOnThisDay(day: LocalDate = LocalDate.now()): List<DisciplinaryActionStringDEO> {
    return lockedTransaction {

        val tA = Teams.alias("tA")
        val tB = Teams.alias("tB")
        TournamentReports
            .leftJoin(Tournaments)
            .leftJoin(GameReports)
            .join(tA, JoinType.LEFT, GameReports.teamA, tA[Teams.id])
            .join(tB, JoinType.LEFT, GameReports.teamB, tB[Teams.id])
            .leftJoin(DisciplinaryActions)
            .leftJoin(Rules)
            .selectAll()
            .adjustWhere {
                TournamentReports.submitDate.between(
                    day.atStartOfDay(),
                    day.plusDays(1).atStartOfDay()
                ) and (
                        Rules.isRed or DisciplinaryActions.redCardIssued
                        )
            }.map {
                val dA = DisciplinaryAction.wrapRow(it)
                val dATeamId = it[DisciplinaryActions.team]
                val teamA = Team.wrapRow(it, tA)
                val teamB = Team.wrapRow(it, tB)
                val tournamentReport = TournamentReport.wrapRow(it)
                val gameReport = GameReport.wrapRow(it)
                val rule = Rule.wrapRow(it)
                val tournament = Tournament.wrapRow(it)

                val opposingTeam = setOf(
                    teamA, teamB
                ).first {
                    it.id != dATeamId
                }
                val dATeam = setOf(
                    teamA, teamB
                ).first {
                    it.id == dATeamId
                }
                val reportShareLink = TournamentReportByIdDEO.fromTournamentReport(
                    tournamentReport
                ).createShareLink()
                    .map { GGERefereeConfig.serverUrl + "/report/share/" + it.uuid }
                    .getOrElse { GGERefereeConfig.serverUrl }

                DisciplinaryActionStringDEO(
                    dA.id.value,
                    dATeam.name,
                    opposingTeam.name,
                    dA.firstName,
                    dA.lastName,
                    dA.number,
                    dA.details,
                    rule.description,
                    tournament.name,
                    tournament.location,
                    gameReport.startTime ?: LocalDateTime.now(),
                    reportShareLink,
                    dA.redCardIssued
                )

            }

    }
}


suspend fun DeleteDisciplinaryActionDEO.deleteChecked(user: User): Result<Boolean> {
    val deleteId = this.id
    return lockedTransaction {
        DisciplinaryAction.findById(deleteId)?.let {
            if (it.game.report.referee.id == user.id) {
                deleteFromDatabase()
            } else {
                Result.failure(IllegalArgumentException("No rights - User is not the referee of this game"))
            }
        } ?: Result.failure(IllegalArgumentException("Disciplinary Action does not exist"))
    }
}

suspend fun DeleteDisciplinaryActionDEO.deleteFromDatabase(): Result<Boolean> {
    val deleteId = this.id
    return lockedTransaction {
        val action = DisciplinaryAction.findById(deleteId)

        if (action != null) {
            val game = action.game
            clearCacheForGameReport(game)

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

suspend fun InjuryDEO.Companion.fromInjury(injury: Injury): InjuryDEO {
    return lockedTransaction {
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

suspend fun InjuryDEO.Companion.wrapRow(row: ResultRow): InjuryDEO {
    return lockedTransaction {
        InjuryDEO(
            row[Injuries.id].value,
            row[Injuries.team].value,
            row[Injuries.firstName],
            row[Injuries.lastName],
            row[Injuries.details],
            row[Injuries.game].value
        )
    }
}


suspend fun InjuryDEO.getRefereeId(): Long? {
    return lockedTransaction {
        TournamentReports.leftJoin(GameReports)
            .selectAll()
            .where { GameReports.id eq this@getRefereeId.game }
            .firstOrNull()
            ?.let {
                it[TournamentReports.referee].value
            }
    }

}

suspend fun InjuryDEO.createInDatabase(): Result<Injury> {
    val injuryUpdate = this
    if (injuryUpdate.team != null &&
        injuryUpdate.firstName != null &&
        injuryUpdate.lastName != null &&
        injuryUpdate.details != null &&
        injuryUpdate.game != null
    ) {

        return lockedTransaction {
            val team = Team.findById(injuryUpdate.team)
            val game = GameReport.findById(injuryUpdate.game)
            if (team != null && game != null) {
                clearCacheForGameReport(game)

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

suspend fun InjuryDEO.updateInDatabase(): Result<Injury> {
    val injuryUpdate = this
    if (injuryUpdate.id != null) {
        return lockedTransaction {
            val injury = Injury.findById(injuryUpdate.id)
            if (injury != null) {
                val game = injury.game
                clearCacheForGameReport(game)

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

private suspend fun Transaction.clearCacheForGameReport(game: GameReport) {
    val report = game.report
    clearCacheForTournamentReport(report)
}


suspend fun DeleteInjuryDEO.deleteChecked(user: User): Result<Boolean> {
    val deleteId = this.id
    return lockedTransaction {
        Injury.findById(deleteId)?.let {
            if (it.game.report.referee.id == user.id) {
                deleteFromDatabase()
            } else {
                Result.failure(IllegalArgumentException("No rights - User is not the referee of this game"))
            }
        } ?: Result.failure(IllegalArgumentException("Injury does not exist"))
    }
}

suspend fun DeleteInjuryDEO.deleteFromDatabase(): Result<Boolean> {
    val deleteId = this.id
    return lockedTransaction {
        val injury = Injury.findById(deleteId)
        if (injury != null) {
            val game = injury.game
            clearCacheForGameReport(game)

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


suspend fun GameTypeDEO.Companion.fromGameType(gameType: GameType): GameTypeDEO {
    return lockedTransaction {
        GameTypeDEO(
            gameType.id.value,
            gameType.name
        )
    }
}

suspend fun GameTypeDEO.createInDatabase(): Result<GameType> {
    GameReportClassesDEO.deleteCache()

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

suspend fun GameTypeDEO.updateInDatabase(): Result<GameType> {
    GameReportClassesDEO.deleteCache()

    val gUpdate = this
    if (gUpdate.id != null) {
        return lockedTransaction {
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


suspend fun CompleteGameReportDEO.Companion.fromGameReport(gameReport: GameReport): CompleteGameReportDEO {
    return lockedTransaction {
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

suspend fun CompleteGameReportDEO.Companion.wrapRow(row: ResultRow): CompleteGameReportDEO {

    val grDEO = GameReportDEO.wrapRow(row)

    return CompleteGameReportDEO(
        gameReport = grDEO,
        injuries = Injuries.selectAll().where { Injuries.game eq grDEO.id }.map {
            InjuryDEO.wrapRow(it)
        },
        disciplinaryActions = DisciplinaryActions.selectAll()
            .where { DisciplinaryActions.game eq grDEO.id }.map {
                DisciplinaryActionDEO.wrapRow(it)
            }
    )


}


suspend fun CompleteGameReportWithRefereeReportDEO.Companion.fromGameReport(gameReport: GameReport): CompleteGameReportWithRefereeReportDEO {
    return lockedTransaction {
        CompleteGameReportWithRefereeReportDEO(
            gameReport = CompleteGameReportDEO.fromGameReport(gameReport),
            refereeReport = CompactTournamentReportDEO.fromTournamentReport(
                gameReport.report
            )
        )
    }
}

suspend fun CompleteGameReportWithRefereeReportDEO.Companion.wrapRow(
    row: ResultRow,
    tournamentReport: TournamentReport
): CompleteGameReportWithRefereeReportDEO {

    val gameReportDEO = CompleteGameReportDEO.wrapRow(row)
    val compactTournamentReportDEO = CompactTournamentReportDEO.wrapRow(
        row
    )
    return CompleteGameReportWithRefereeReportDEO(
        gameReport = gameReportDEO,
        refereeReport = compactTournamentReportDEO
    )

}

suspend fun CompleteTournamentReportDEO.Companion.wrapRow(
    row: ResultRow,
    tournamentReport: TournamentReport
): CompleteGameReportWithRefereeReportDEO {
    return lockedTransaction {
        CompleteGameReportWithRefereeReportDEO(
            gameReport = CompleteGameReportDEO.wrapRow(row),
            refereeReport = CompactTournamentReportDEO.wrapRow(
                row
            )
        )
    }
}

suspend fun PublicDisciplinaryActionDEO.Companion.fromDisciplinaryAction(disciplinaryAction: DisciplinaryAction): PublicDisciplinaryActionDEO {
    return lockedTransaction {
        PublicDisciplinaryActionDEO(
            id = disciplinaryAction.id.value,
            team = disciplinaryAction.team.id.value,
            rule = disciplinaryAction.rule.id.value,
            game = disciplinaryAction.game.id.value,
            redCardIssued = disciplinaryAction.redCardIssued
        )
    }
}

suspend fun PublicDisciplinaryActionDEO.Companion.wrapRow(row: ResultRow): PublicDisciplinaryActionDEO? {
    if (row.getOrNull(DisciplinaryActions.id) == null) {
        return null
    }
    return PublicDisciplinaryActionDEO(
        id = row[DisciplinaryActions.id].value,
        team = row[DisciplinaryActions.team].value,
        rule = row[DisciplinaryActions.rule].value,
        game = row[DisciplinaryActions.game].value,
        redCardIssued = row[DisciplinaryActions.redCardIssued]
    )
}

suspend fun PublicGameReportDEO.Companion.fromGameReport(gameReport: GameReport): PublicGameReportDEO {
    return lockedTransaction {
        val dAs = gameReport.disciplinaryActions.map {
            PublicDisciplinaryActionDEO.fromDisciplinaryAction(
                it
            )
        }
        PublicGameReportDEO(
            gameReport = GameReportDEO.fromGameReport(gameReport),
            disciplinaryActions = dAs,
            code = gameReport.report.code.id.value
        )


    }
}




