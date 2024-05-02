package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.Tournament
import eu.gaelicgames.referee.data.TournamentTeamPreSelections
import eu.gaelicgames.referee.util.lockedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

suspend fun TournamentTeamPreselectionDEO.Companion.fromTournamentId(id: Long): Result<TournamentTeamPreselectionDEO> {
    return lockedTransaction {
        val tournament = Tournament.findById(id)
            ?: return@lockedTransaction Result.failure(IllegalArgumentException("Tournament with id $id does not exist"))
        val preselections = TournamentTeamPreSelections.selectAll()
            .where { TournamentTeamPreSelections.tournament eq tournament.id }
            .map { it[TournamentTeamPreSelections.team].value }
        return@lockedTransaction Result.success(TournamentTeamPreselectionDEO(tournament.id.value, preselections))

    }
}

suspend fun TournamentTeamPreselectionDEO.add(): Result<TournamentTeamPreselectionDEO> {
    return set(false)
}

suspend fun TournamentTeamPreselectionDEO.set(deleteMissing: Boolean): Result<TournamentTeamPreselectionDEO> {
    val updateDEO = this
    return lockedTransaction {
        val tournament = Tournament.findById(updateDEO.tournamentId)
            ?: return@lockedTransaction Result.failure(IllegalArgumentException("Tournament with id ${updateDEO.tournamentId} does not exist"))
        val storedIds = TournamentTeamPreSelections.selectAll()
            .where { TournamentTeamPreSelections.tournament eq tournament.id }
            .map { it[TournamentTeamPreSelections.team].value }
        val newIds = updateDEO.teamIds.filter { it !in storedIds }
        if (deleteMissing) {
            val deleteIds = storedIds.filter { it !in updateDEO.teamIds }
            TournamentTeamPreSelections.deleteWhere {
                TournamentTeamPreSelections.tournament eq tournament.id and
                        (TournamentTeamPreSelections.id inList deleteIds)
            }
        }
        newIds.forEach { id->
            TournamentTeamPreSelections.insert {
                it[this.tournament] = tournament.id
                it[this.team] = id
            }
        }

        return@lockedTransaction TournamentTeamPreselectionDEO.fromTournamentId(tournament.id.value)
    }
}

suspend fun TournamentTeamPreselectionDEO.deleteTeams(): Result<TournamentTeamPreselectionDEO> {
    val updateDEO = this
    return lockedTransaction {
        val tournament = Tournament.findById(updateDEO.tournamentId)
            ?: return@lockedTransaction Result.failure(IllegalArgumentException("Tournament with id ${updateDEO.tournamentId} does not exist"))
        TournamentTeamPreSelections.deleteWhere {
            TournamentTeamPreSelections.tournament eq tournament.id and
                    (TournamentTeamPreSelections.team inList updateDEO.teamIds)
        }
        return@lockedTransaction TournamentTeamPreselectionDEO.fromTournamentId(tournament.id.value)
    }
}

suspend fun GetTournamentTeamPreselectionDEO.load(): Result<TournamentTeamPreselectionDEO> {
    return TournamentTeamPreselectionDEO.fromTournamentId(this.tournamentId)
}
