package eu.gaelicgames.referee.util

import eu.gaelicgames.referee.data.Team
import eu.gaelicgames.referee.data.Teams
import eu.gaelicgames.referee.data.api.createTeamAliasInTransaction
import java.time.LocalDate

/**
 * Turns every already-merged team's name into a search alias of the team that
 * survived the merge, so the alias set is useful on day one instead of waiting
 * for an admin to type it all in.
 *
 * Idempotent: aliases that already exist are rejected by the collision check
 * and silently skipped. Returns the number of aliases created.
 */
suspend fun backfillAliasesFromMergedTeams(): Int {
    return lockedTransaction {
        var created = 0
        val mergedTeams = Team.find { Teams.mergedInto.isNotNull() }.toList()
        for (deadTeam in mergedTeams) {
            val survivor = resolveSurvivor(deadTeam) ?: continue
            if (normalizeTeamName(survivor.name) == normalizeTeamName(deadTeam.name)) {
                continue
            }
            val result = createTeamAliasInTransaction(
                survivor, deadTeam.name, LocalDate.now(), null
            )
            if (result.isSuccess) {
                created++
            }
        }
        created
    }
}

/**
 * Follows the mergedInto chain to the team that is still live.
 * Returns null on a cycle or a chain that ends in another dead team.
 */
private fun resolveSurvivor(start: Team): Team? {
    var current: Team = start.mergedInto ?: return null
    val seen = mutableSetOf(start.id.value)
    while (true) {
        if (!seen.add(current.id.value)) {
            return null
        }
        val next = current.mergedInto ?: return if (current.deletedAt == null) current else null
        current = next
    }
}
