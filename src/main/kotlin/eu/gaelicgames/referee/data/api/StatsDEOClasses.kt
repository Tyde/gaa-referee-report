package eu.gaelicgames.referee.data.api

import kotlinx.serialization.Serializable


@Serializable
data class TeamTournamentCountDEO(
    val teamId: Long,
    val teamName: String,
    val tournamentCount: Int
)

@Serializable
data class CardStatsByYearDEO(
    val year: Int,
    val cautionCount: Int,
    val blackCardCount: Int,
    val redCardCount: Int,
    val totalGames: Int
)

@Serializable
data class CardStatsByRegionDEO(
    val regionId: Long,
    val regionName: String,
    val cautionCount: Int,
    val blackCardCount: Int,
    val redCardCount: Int,
    val totalGames: Int
)

@Serializable
data class AverageCardsPerGameDEO(
    val averageCautions: Double,
    val averageBlackCards: Double,
    val averageRedCards: Double,
    val totalGames: Int
)

@Serializable
data class TeamEloDEO(
    val teamId: Long,
    val teamName: String,
    val eloScore: Double,
    val gamesPlayed: Int
)

@Serializable
data class StatsDEO(
    val teamTournamentCounts: List<TeamTournamentCountDEO>,
    val cardsByYear: List<CardStatsByYearDEO>,
    val cardsByRegion: List<CardStatsByRegionDEO>,
    val averageCardsPerGame: AverageCardsPerGameDEO,
    val teamElos: List<TeamEloDEO>
)
