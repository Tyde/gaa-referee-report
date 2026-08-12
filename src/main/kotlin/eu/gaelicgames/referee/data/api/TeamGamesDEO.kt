package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.Amalgamations
import eu.gaelicgames.referee.data.GameCodes
import eu.gaelicgames.referee.data.GameReports
import eu.gaelicgames.referee.data.GameTypes
import eu.gaelicgames.referee.data.Teams
import eu.gaelicgames.referee.data.TournamentReports
import eu.gaelicgames.referee.data.Tournaments
import eu.gaelicgames.referee.data.Users
import eu.gaelicgames.referee.util.lockedTransaction
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll

suspend fun TeamGamesDEO.Companion.forTeam(
    teamId: Long,
    includeAmalgamatedTeams: Boolean
): Result<TeamGamesDEO> = lockedTransaction {
    val selectedTeamRow = Teams.selectAll()
        .where { (Teams.id eq teamId) and Teams.deletedAt.isNull() }
        .singleOrNull()
        ?: return@lockedTransaction Result.failure(IllegalArgumentException("Team not found"))

    val selectedTeam = selectedTeamRow.toTeamDEO()
    val includedAmalgamations = if (includeAmalgamatedTeams && !selectedTeam.isAmalgamation) {
        Amalgamations
            .innerJoin(Teams, { Amalgamations.amalgamation }, { Teams.id })
            .selectAll()
            .where {
                (Amalgamations.addedTeam eq teamId) and
                    (Teams.isAmalgamation eq true) and
                    Teams.deletedAt.isNull()
            }
            .map { it.toTeamDEO() }
    } else {
        emptyList()
    }
    val effectiveTeamIds = (includedAmalgamations.map { it.id } + selectedTeam.id).toSet()

    val teamA = Teams.alias("team_games_team_a")
    val teamB = Teams.alias("team_games_team_b")
    val games = GameReports
        .innerJoin(TournamentReports, { GameReports.report }, { TournamentReports.id })
        .innerJoin(Tournaments, { TournamentReports.tournament }, { Tournaments.id })
        .innerJoin(Users, { TournamentReports.referee }, { Users.id })
        .innerJoin(GameCodes, { TournamentReports.code }, { GameCodes.id })
        .join(teamA, JoinType.INNER, GameReports.teamA, teamA[Teams.id])
        .join(teamB, JoinType.INNER, GameReports.teamB, teamB[Teams.id])
        .join(GameTypes, JoinType.LEFT, GameReports.gameType, GameTypes.id)
        .selectAll()
        .where {
            (TournamentReports.isSubmitted eq true) and
                ((GameReports.teamA inList effectiveTeamIds.toList()) or
                    (GameReports.teamB inList effectiveTeamIds.toList()))
        }
        .map { row -> row.toTeamGameDEO(teamId, effectiveTeamIds, teamA, teamB) }
        .distinctBy { it.gameId }

    Result.success(TeamGamesDEO(selectedTeam, includedAmalgamations, games))
}

private fun ResultRow.toTeamDEO(): TeamDEO = TeamDEO(
    name = this[Teams.name],
    id = this[Teams.id].value,
    isAmalgamation = this[Teams.isAmalgamation],
    amalgamationTeams = null
)

private fun ResultRow.toTeamGameDEO(
    selectedTeamId: Long,
    effectiveTeamIds: Set<Long>,
    teamA: org.jetbrains.exposed.sql.Alias<eu.gaelicgames.referee.data.Teams>,
    teamB: org.jetbrains.exposed.sql.Alias<eu.gaelicgames.referee.data.Teams>
): TeamGameDEO {
    val teamAId = this[teamA[Teams.id]].value
    val teamBId = this[teamB[Teams.id]].value
    val playAsTeamA = when {
        teamAId == selectedTeamId -> true
        teamBId == selectedTeamId -> false
        teamAId in effectiveTeamIds -> true
        else -> false
    }
    val playedAsTeam = if (playAsTeamA) this.toTeamDEO(teamA) else this.toTeamDEO(teamB)
    val opponentTeam = if (playAsTeamA) this.toTeamDEO(teamB) else this.toTeamDEO(teamA)

    return TeamGameDEO(
        gameId = this[GameReports.id].value,
        reportId = this[GameReports.report].value,
        tournament = TournamentDEO(
            id = this[Tournaments.id].value,
            name = this[Tournaments.name],
            location = this[Tournaments.location],
            date = this[Tournaments.date],
            region = this[Tournaments.region].value,
            isLeague = this[Tournaments.isLeague],
            endDate = this[Tournaments.endDate]
        ),
        startTime = this[GameReports.startTime],
        playedAsTeam = playedAsTeam,
        opponentTeam = opponentTeam,
        playedAsGoals = if (playAsTeamA) this[GameReports.teamAGoals] else this[GameReports.teamBGoals],
        playedAsPoints = if (playAsTeamA) this[GameReports.teamAPoints] else this[GameReports.teamBPoints],
        opponentGoals = if (playAsTeamA) this[GameReports.teamBGoals] else this[GameReports.teamAGoals],
        opponentPoints = if (playAsTeamA) this[GameReports.teamBPoints] else this[GameReports.teamAPoints],
        refereeId = this[TournamentReports.referee].value,
        refereeName = "${this[Users.firstName]} ${this[Users.lastName]}",
        codeId = this[TournamentReports.code].value,
        codeName = this[GameCodes.name],
        gameTypeId = this[GameReports.gameType]?.value,
        gameTypeName = this.getOrNull(GameTypes.name)
    )
}

private fun ResultRow.toTeamDEO(team: org.jetbrains.exposed.sql.Alias<eu.gaelicgames.referee.data.Teams>): TeamDEO = TeamDEO(
    name = this[team[Teams.name]],
    id = this[team[Teams.id]].value,
    isAmalgamation = this[team[Teams.isAmalgamation]],
    amalgamationTeams = null
)
