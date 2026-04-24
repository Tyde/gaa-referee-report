package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.Amalgamations
import eu.gaelicgames.referee.data.DisciplinaryActions
import eu.gaelicgames.referee.data.GameReports
import eu.gaelicgames.referee.data.Regions
import eu.gaelicgames.referee.data.Rules
import eu.gaelicgames.referee.data.Teams
import eu.gaelicgames.referee.data.TournamentReports
import eu.gaelicgames.referee.data.Tournaments
import eu.gaelicgames.referee.util.lockedTransaction
import org.jetbrains.exposed.sql.*
import kotlin.math.pow
import kotlin.math.roundToLong


private fun buildSingleTeamAmalgamationMap(): Map<Long, Long> {
    val result = mutableMapOf<Long, Long>()
    val baseTeamNames = mutableMapOf<Long, String>()
    
    Amalgamations
        .innerJoin(Teams, { Amalgamations.addedTeam }, { Teams.id })
        .select(Amalgamations.addedTeam, Teams.name)
        .forEach { row ->
            baseTeamNames[row[Amalgamations.addedTeam].value] = row[Teams.name]
        }
    
    Amalgamations
        .innerJoin(Teams, { Amalgamations.amalgamation }, { Teams.id })
        .select(Amalgamations.amalgamation, Amalgamations.addedTeam, Teams.name)
        .where { Teams.isAmalgamation eq true }
        .groupBy { it[Amalgamations.amalgamation].value }
        .forEach { (amalgamationId, rows) ->
            if (rows.size == 1) {
                val baseTeamId = rows[0][Amalgamations.addedTeam].value
                val amalgamationName = rows[0][Teams.name]
                val baseName = baseTeamNames[baseTeamId] ?: ""
                
                if (isSquadATeam(amalgamationName, baseName)) {
                    result[amalgamationId] = baseTeamId
                }
            }
        }
    return result
}

private fun isSquadATeam(amalgamationName: String, baseName: String): Boolean {
    val normalizedAmalgamation = amalgamationName.trim().lowercase()
    val normalizedBase = baseName.trim().lowercase()
    
    val patterns = listOf(
        normalizedBase + " a",
        normalizedBase + "a",
        normalizedBase + " a-team",
        normalizedBase + " a team",
        normalizedBase + "-a",
        normalizedBase + " a squad"
    )
    
    return patterns.any { normalizedAmalgamation == it } ||
           (normalizedAmalgamation.startsWith(normalizedBase) && 
            normalizedAmalgamation.substring(normalizedBase.length).trim() in listOf("a", " a", "-a", " a-team", " a team"))
}

suspend fun StatsDEO.Companion.load(): StatsDEO {
    return lockedTransaction {
        val singleTeamAmalgamationMap = buildSingleTeamAmalgamationMap()
        val teamTournamentCounts = computeTeamTournamentCounts(singleTeamAmalgamationMap)
        val cardsByYear = computeCardsByYear()
        val cardsByRegion = computeCardsByRegion()
        val averageCardsPerGame = computeAverageCardsPerGame()
        val teamElos = computeTeamElos(singleTeamAmalgamationMap)
        StatsDEO(
            teamTournamentCounts = teamTournamentCounts,
            cardsByYear = cardsByYear,
            cardsByRegion = cardsByRegion,
            averageCardsPerGame = averageCardsPerGame,
            teamElos = teamElos
        )
    }
}

private fun computeTeamTournamentCounts(singleTeamAmalgamationMap: Map<Long, Long>): List<TeamTournamentCountDEO> {
    // Count distinct tournaments each team participated in (as teamA or teamB in submitted reports)

    // Get all (teamId, tournamentId) pairs from submitted tournament reports
    val teamTournamentPairs = mutableSetOf<Pair<Long, Long>>()

    GameReports
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .select(GameReports.teamA, GameReports.teamB, TournamentReports.tournament)
        .where { TournamentReports.isSubmitted eq true }
        .forEach { row ->
            val tournamentId = row[TournamentReports.tournament].value
            val teamAId = row[GameReports.teamA].value
            val teamBId = row[GameReports.teamB].value
            val effectiveTeamAId = singleTeamAmalgamationMap[teamAId] ?: teamAId
            val effectiveTeamBId = singleTeamAmalgamationMap[teamBId] ?: teamBId
            teamTournamentPairs.add(Pair(effectiveTeamAId, tournamentId))
            teamTournamentPairs.add(Pair(effectiveTeamBId, tournamentId))
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
    )

    val statsByYear = mutableMapOf<Int, YearStats>()

    DisciplinaryActions
        .innerJoin(GameReports, { DisciplinaryActions.game }, { GameReports.id })
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .innerJoin(Tournaments, { TournamentReports.tournament }, { Tournaments.id })
        .innerJoin(Rules, { DisciplinaryActions.rule }, { Rules.id })
        .select(
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
            when {
                row[Rules.isRed] || row[DisciplinaryActions.redCardIssued] -> stats.redCardCount++
                row[Rules.isBlack] -> stats.blackCardCount++
                row[Rules.isCaution] -> stats.cautionCount++
            }
        }

    // Count all games (including those with no cards) to get total games per year
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

    return gamesByYear.keys.sorted().map { year ->
        val stats = statsByYear[year]
        CardStatsByYearDEO(
            year = year,
            cautionCount = stats?.cautionCount ?: 0,
            blackCardCount = stats?.blackCardCount ?: 0,
            redCardCount = stats?.redCardCount ?: 0,
            totalGames = gamesByYear[year]?.size ?: 0
        )
    }
}

private fun computeCardsByRegion(): List<CardStatsByRegionDEO> {
    data class RegionStats(
        var cautionCount: Int = 0,
        var blackCardCount: Int = 0,
        var redCardCount: Int = 0,
    )

    val statsByRegion = mutableMapOf<Long, RegionStats>()

    DisciplinaryActions
        .innerJoin(GameReports, { DisciplinaryActions.game }, { GameReports.id })
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .innerJoin(Tournaments, { TournamentReports.tournament }, { Tournaments.id })
        .innerJoin(Regions, { Tournaments.region }, { Regions.id })
        .innerJoin(Rules, { DisciplinaryActions.rule }, { Rules.id })
        .select(
            Regions.id,
            Rules.isCaution,
            Rules.isBlack,
            Rules.isRed,
            DisciplinaryActions.redCardIssued
        )
        .where { TournamentReports.isSubmitted eq true }
        .forEach { row ->
            val regionId = row[Regions.id].value
            val stats = statsByRegion.getOrPut(regionId) { RegionStats() }
            when {
                row[Rules.isRed] || row[DisciplinaryActions.redCardIssued] -> stats.redCardCount++
                row[Rules.isBlack] -> stats.blackCardCount++
                row[Rules.isCaution] -> stats.cautionCount++
            }
        }

    // Count total games per region and collect region names
    val gamesByRegion = mutableMapOf<Long, MutableSet<Long>>()
    val regionNames = mutableMapOf<Long, String>()
    GameReports
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .innerJoin(Tournaments, { TournamentReports.tournament }, { Tournaments.id })
        .innerJoin(Regions, { Tournaments.region }, { Regions.id })
        .select(GameReports.id, Tournaments.region, Regions.name)
        .where { TournamentReports.isSubmitted eq true }
        .forEach { row ->
            val regionId = row[Tournaments.region].value
            regionNames[regionId] = row[Regions.name]
            gamesByRegion.getOrPut(regionId) { mutableSetOf() }.add(row[GameReports.id].value)
        }

    return gamesByRegion.keys.map { regionId ->
        val stats = statsByRegion[regionId]
        CardStatsByRegionDEO(
            regionId = regionId,
            regionName = regionNames[regionId] ?: "Unknown",
            cautionCount = stats?.cautionCount ?: 0,
            blackCardCount = stats?.blackCardCount ?: 0,
            redCardCount = stats?.redCardCount ?: 0,
            totalGames = gamesByRegion[regionId]?.size ?: 0
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

private fun gaaScore(goals: Int, points: Int) = goals * 3 + points

private fun computeTeamElos(singleTeamAmalgamationMap: Map<Long, Long>): List<TeamEloDEO> {
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
            val teamAScore = gaaScore(row[GameReports.teamAGoals], row[GameReports.teamAPoints])
            val teamBScore = gaaScore(row[GameReports.teamBGoals], row[GameReports.teamBPoints])
            val rawTeamAId = row[GameReports.teamA].value
            val rawTeamBId = row[GameReports.teamB].value
            val effectiveTeamAId = singleTeamAmalgamationMap[rawTeamAId] ?: rawTeamAId
            val effectiveTeamBId = singleTeamAmalgamationMap[rawTeamBId] ?: rawTeamBId
            GameResult(
                tournamentDate = row[Tournaments.date],
                teamAId = effectiveTeamAId,
                teamBId = effectiveTeamBId,
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
                eloScore = (elo * 10.0).roundToLong() / 10.0,
                gamesPlayed = gamesPlayed[teamId] ?: 0
            )
        }
        .sortedByDescending { it.eloScore }
}
