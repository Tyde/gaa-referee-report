package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.lockedTransaction
import org.jetbrains.exposed.sql.*
import kotlin.math.pow


suspend fun StatsDEO.Companion.load(): StatsDEO {
    return lockedTransaction {
        val teamTournamentCounts = computeTeamTournamentCounts()
        val cardsByYear = computeCardsByYear()
        val cardsByRegion = computeCardsByRegion()
        val averageCardsPerGame = computeAverageCardsPerGame()
        val teamElos = computeTeamElos()
        StatsDEO(
            teamTournamentCounts = teamTournamentCounts,
            cardsByYear = cardsByYear,
            cardsByRegion = cardsByRegion,
            averageCardsPerGame = averageCardsPerGame,
            teamElos = teamElos
        )
    }
}

private fun computeTeamTournamentCounts(): List<TeamTournamentCountDEO> {
    // Count distinct tournaments each team participated in (as teamA or teamB in submitted reports)

    // Get all (teamId, tournamentId) pairs from submitted tournament reports
    val teamTournamentPairs = mutableSetOf<Pair<Long, Long>>()

    GameReports
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .select(GameReports.teamA, GameReports.teamB, TournamentReports.tournament)
        .where { TournamentReports.isSubmitted eq true }
        .forEach { row ->
            val tournamentId = row[TournamentReports.tournament].value
            teamTournamentPairs.add(Pair(row[GameReports.teamA].value, tournamentId))
            teamTournamentPairs.add(Pair(row[GameReports.teamB].value, tournamentId))
        }

    // Count tournaments per team
    val countByTeam = teamTournamentPairs
        .groupBy { it.first }
        .mapValues { it.value.size }

    // Fetch team names
    val teamIds = countByTeam.keys.toList()
    if (teamIds.isEmpty()) return emptyList()

    val teamNames = Teams.selectAll()
        .where { Teams.id inList teamIds }
        .associate { it[Teams.id].value to it[Teams.name] }

    return countByTeam
        .map { (teamId, count) ->
            TeamTournamentCountDEO(
                teamId = teamId,
                teamName = teamNames[teamId] ?: "Unknown",
                tournamentCount = count
            )
        }
        .sortedByDescending { it.tournamentCount }
}

private fun computeCardsByYear(): List<CardStatsByYearDEO> {
    // Join DisciplinaryActions → GameReports → TournamentReports → Tournaments
    // Group by year of tournament date
    data class YearStats(
        var cautionCount: Int = 0,
        var blackCardCount: Int = 0,
        var redCardCount: Int = 0,
        val gameIds: MutableSet<Long> = mutableSetOf()
    )

    val statsByYear = mutableMapOf<Int, YearStats>()

    DisciplinaryActions
        .innerJoin(GameReports, { DisciplinaryActions.game }, { GameReports.id })
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .innerJoin(Tournaments, { TournamentReports.tournament }, { Tournaments.id })
        .innerJoin(Rules, { DisciplinaryActions.rule }, { Rules.id })
        .select(
            DisciplinaryActions.id,
            GameReports.id,
            Tournaments.date,
            Rules.isCaution,
            Rules.isBlack,
            Rules.isRed,
            DisciplinaryActions.redCardIssued
        )
        .where { TournamentReports.isSubmitted eq true }
        .forEach { row ->
            val year = row[Tournaments.date].year
            val stats = statsByYear.getOrPut(year) { YearStats() }
            stats.gameIds.add(row[GameReports.id].value)
            when {
                row[Rules.isRed] || row[DisciplinaryActions.redCardIssued] -> stats.redCardCount++
                row[Rules.isBlack] -> stats.blackCardCount++
                row[Rules.isCaution] -> stats.cautionCount++
            }
        }

    // Also count games without any cards to get total games per year
    val gamesByYear = mutableMapOf<Int, MutableSet<Long>>()
    GameReports
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .innerJoin(Tournaments, { TournamentReports.tournament }, { Tournaments.id })
        .select(GameReports.id, Tournaments.date)
        .where { TournamentReports.isSubmitted eq true }
        .forEach { row ->
            val year = row[Tournaments.date].year
            gamesByYear.getOrPut(year) { mutableSetOf() }.add(row[GameReports.id].value)
        }

    return statsByYear.map { (year, stats) ->
        CardStatsByYearDEO(
            year = year,
            cautionCount = stats.cautionCount,
            blackCardCount = stats.blackCardCount,
            redCardCount = stats.redCardCount,
            totalGames = gamesByYear[year]?.size ?: stats.gameIds.size
        )
    }.sortedBy { it.year }
}

private fun computeCardsByRegion(): List<CardStatsByRegionDEO> {
    data class RegionStats(
        val regionName: String,
        var cautionCount: Int = 0,
        var blackCardCount: Int = 0,
        var redCardCount: Int = 0,
        val gameIds: MutableSet<Long> = mutableSetOf()
    )

    val statsByRegion = mutableMapOf<Long, RegionStats>()

    DisciplinaryActions
        .innerJoin(GameReports, { DisciplinaryActions.game }, { GameReports.id })
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .innerJoin(Tournaments, { TournamentReports.tournament }, { Tournaments.id })
        .innerJoin(Regions, { Tournaments.region }, { Regions.id })
        .innerJoin(Rules, { DisciplinaryActions.rule }, { Rules.id })
        .select(
            DisciplinaryActions.id,
            GameReports.id,
            Regions.id,
            Regions.name,
            Rules.isCaution,
            Rules.isBlack,
            Rules.isRed,
            DisciplinaryActions.redCardIssued
        )
        .where { TournamentReports.isSubmitted eq true }
        .forEach { row ->
            val regionId = row[Regions.id].value
            val stats = statsByRegion.getOrPut(regionId) {
                RegionStats(regionName = row[Regions.name])
            }
            stats.gameIds.add(row[GameReports.id].value)
            when {
                row[Rules.isRed] || row[DisciplinaryActions.redCardIssued] -> stats.redCardCount++
                row[Rules.isBlack] -> stats.blackCardCount++
                row[Rules.isCaution] -> stats.cautionCount++
            }
        }

    // Count total games per region
    val gamesByRegion = mutableMapOf<Long, MutableSet<Long>>()
    GameReports
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .innerJoin(Tournaments, { TournamentReports.tournament }, { Tournaments.id })
        .select(GameReports.id, Tournaments.region)
        .where { TournamentReports.isSubmitted eq true }
        .forEach { row ->
            val regionId = row[Tournaments.region].value
            gamesByRegion.getOrPut(regionId) { mutableSetOf() }.add(row[GameReports.id].value)
        }

    return statsByRegion.map { (regionId, stats) ->
        CardStatsByRegionDEO(
            regionId = regionId,
            regionName = stats.regionName,
            cautionCount = stats.cautionCount,
            blackCardCount = stats.blackCardCount,
            redCardCount = stats.redCardCount,
            totalGames = gamesByRegion[regionId]?.size ?: stats.gameIds.size
        )
    }.sortedBy { it.regionName }
}

private fun computeAverageCardsPerGame(): AverageCardsPerGameDEO {
    var cautionCount = 0
    var blackCardCount = 0
    var redCardCount = 0

    DisciplinaryActions
        .innerJoin(GameReports, { DisciplinaryActions.game }, { GameReports.id })
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .innerJoin(Rules, { DisciplinaryActions.rule }, { Rules.id })
        .select(Rules.isCaution, Rules.isBlack, Rules.isRed, DisciplinaryActions.redCardIssued)
        .where { TournamentReports.isSubmitted eq true }
        .forEach { row ->
            when {
                row[Rules.isRed] || row[DisciplinaryActions.redCardIssued] -> redCardCount++
                row[Rules.isBlack] -> blackCardCount++
                row[Rules.isCaution] -> cautionCount++
            }
        }

    val totalGames = GameReports
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .select(GameReports.id)
        .where { TournamentReports.isSubmitted eq true }
        .count()
        .toInt()

    return if (totalGames > 0) {
        AverageCardsPerGameDEO(
            averageCautions = cautionCount.toDouble() / totalGames,
            averageBlackCards = blackCardCount.toDouble() / totalGames,
            averageRedCards = redCardCount.toDouble() / totalGames,
            totalGames = totalGames
        )
    } else {
        AverageCardsPerGameDEO(0.0, 0.0, 0.0, 0)
    }
}

private fun computeTeamElos(): List<TeamEloDEO> {
    data class GameResult(
        val tournamentDate: java.time.LocalDate,
        val teamAId: Long,
        val teamBId: Long,
        val teamAScore: Int,
        val teamBScore: Int
    )

    // Load all games from submitted reports, ordered by tournament date
    val games = GameReports
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .innerJoin(Tournaments, { TournamentReports.tournament }, { Tournaments.id })
        .select(
            GameReports.teamA,
            GameReports.teamB,
            GameReports.teamAGoals,
            GameReports.teamAPoints,
            GameReports.teamBGoals,
            GameReports.teamBPoints,
            Tournaments.date
        )
        .where { TournamentReports.isSubmitted eq true }
        .map { row ->
            val teamAScore = row[GameReports.teamAGoals] * 3 + row[GameReports.teamAPoints]
            val teamBScore = row[GameReports.teamBGoals] * 3 + row[GameReports.teamBPoints]
            GameResult(
                tournamentDate = row[Tournaments.date],
                teamAId = row[GameReports.teamA].value,
                teamBId = row[GameReports.teamB].value,
                teamAScore = teamAScore,
                teamBScore = teamBScore
            )
        }
        .sortedBy { it.tournamentDate }

    if (games.isEmpty()) return emptyList()

    // Get all team names involved
    val teamIds = games.flatMap { listOf(it.teamAId, it.teamBId) }.toSet().toList()
    val teamNames = Teams.selectAll()
        .where { Teams.id inList teamIds }
        .associate { it[Teams.id].value to it[Teams.name] }

    // Calculate ELO ratings
    val eloRatings = mutableMapOf<Long, Double>()
    val gamesPlayed = mutableMapOf<Long, Int>()
    val kFactor = 32.0
    val initialElo = 1000.0

    fun getElo(teamId: Long) = eloRatings.getOrDefault(teamId, initialElo)

    for (game in games) {
        val eloA = getElo(game.teamAId)
        val eloB = getElo(game.teamBId)

        val expectedA = 1.0 / (1.0 + 10.0.pow((eloB - eloA) / 400.0))
        val expectedB = 1.0 - expectedA

        val actualA = when {
            game.teamAScore > game.teamBScore -> 1.0
            game.teamAScore < game.teamBScore -> 0.0
            else -> 0.5
        }
        val actualB = 1.0 - actualA

        eloRatings[game.teamAId] = eloA + kFactor * (actualA - expectedA)
        eloRatings[game.teamBId] = eloB + kFactor * (actualB - expectedB)
        gamesPlayed[game.teamAId] = (gamesPlayed[game.teamAId] ?: 0) + 1
        gamesPlayed[game.teamBId] = (gamesPlayed[game.teamBId] ?: 0) + 1
    }

    return eloRatings
        .filter { (teamId, _) -> (gamesPlayed[teamId] ?: 0) > 0 }
        .map { (teamId, elo) ->
            TeamEloDEO(
                teamId = teamId,
                teamName = teamNames[teamId] ?: "Unknown",
                eloScore = Math.round(elo * 10.0) / 10.0,
                gamesPlayed = gamesPlayed[teamId] ?: 0
            )
        }
        .sortedByDescending { it.eloScore }
}
