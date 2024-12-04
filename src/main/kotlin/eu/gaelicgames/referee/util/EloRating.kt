package eu.gaelicgames.referee.util

import eu.gaelicgames.referee.data.GameReport
import eu.gaelicgames.referee.data.Team
import kotlin.math.pow

data class RankedTeam(val team:Team, var rating: Double = 1500.0)
class EloSystem(private val kFactor: Double = 32.0) {

    fun updateRatings(game: GameReport, teams: List<RankedTeam>) {
        val teamA = teams.find { it.team.id == game.teamA.id }!!
        val teamB = teams.find { it.team.id == game.teamB.id }!!
        val expectedScoreA = expectedScore(teamA.rating, teamB.rating)
        val expectedScoreB = 1.0 - expectedScoreA

        val actualScoreA = calculateActualScore(game.teamAGoals, game.teamAPoints, game.teamBGoals, game.teamBPoints)
        val actualScoreB = 1.0 - actualScoreA

        teamA.rating += kFactor * (actualScoreA - expectedScoreA)
        teamB.rating += kFactor * (actualScoreB - expectedScoreB)
    }

    private fun expectedScore(ratingA: Double, ratingB: Double): Double {
        return 1.0 / (1.0 + 10.0.pow((ratingB - ratingA) / 400.0))
    }

    private fun calculateActualScore(goalsA: Int, pointsA: Int, goalsB: Int, pointsB: Int): Double {
        val scoreA = goalsA * 3 + pointsA
        val scoreB = goalsB * 3 + pointsB
        val scoreDiff = scoreA - scoreB
        return 1.0 / (1.0 + Math.E.pow(-0.1 * scoreDiff))
    }
}
