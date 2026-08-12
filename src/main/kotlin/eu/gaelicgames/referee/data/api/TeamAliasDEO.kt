package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.lockedTransaction
import eu.gaelicgames.referee.util.normalizeTeamName
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.time.LocalDateTime

fun TeamAlias.toDEO(): TeamAliasDEO = TeamAliasDEO(
    id = id.value,
    teamId = team.id.value,
    alias = alias
)

/**
 * Validates an alias against every other alias and every live team's canonical
 * name. Requires an active transaction. Returns the normalized form on success.
 *
 * @param excludeAliasId alias row being updated, excluded from the collision check
 * @param excludeTeamId live team whose canonical name is allowed during an atomic merge
 */
fun validateAliasInTransaction(
    aliasText: String,
    excludeAliasId: Long? = null,
    excludeTeamId: Long? = null
): Result<String> {
    val trimmed = aliasText.trim()
    if (trimmed.isBlank()) {
        return Result.failure(Exception("Alias must not be blank"))
    }
    if (trimmed.length > 100) {
        return Result.failure(Exception("Alias must be at most 100 characters"))
    }
    val normalized = normalizeTeamName(trimmed)

    val clashingAlias = TeamAlias.find { TeamAliases.normalized eq normalized }
        .firstOrNull { it.id.value != excludeAliasId }
    if (clashingAlias != null) {
        return Result.failure(
            Exception("\"$trimmed\" is already an alias of ${clashingAlias.team.name}")
        )
    }

    // Team counts are in the hundreds, so normalizing in memory is cheaper than
    // storing and maintaining a normalized column on Teams.
    val clashingTeam = Team.find { Teams.deletedAt.isNull() }
        .firstOrNull {
            it.id.value != excludeTeamId && normalizeTeamName(it.name) == normalized
        }
    if (clashingTeam != null) {
        return Result.failure(
            Exception("\"$trimmed\" is already the name of team ${clashingTeam.name}")
        )
    }

    return Result.success(normalized)
}

/**
 * Creates an alias inside an existing transaction, writing an ALIAS_ADDED
 * history event. Used directly by merge, rename and the backfill migration so
 * the alias is committed atomically with the change that proposed it.
 */
fun createTeamAliasInTransaction(
    team: Team,
    aliasText: String,
    changeDate: LocalDate,
    recordedBy: User? = null,
    excludeTeamId: Long? = null
): Result<TeamAlias> {
    return validateAliasInTransaction(aliasText, excludeTeamId = excludeTeamId).map { normalizedValue ->
        val trimmed = aliasText.trim()
        val created = TeamAlias.new {
            this.team = team
            alias = trimmed
            normalized = normalizedValue
            createdAt = LocalDateTime.now()
            createdBy = recordedBy
        }
        writeTeamHistoryEventInTransaction(
            team, TeamChangeType.ALIAS_ADDED, changeDate, null, trimmed, recordedBy
        )
        created
    }
}

suspend fun NewTeamAliasDEO.createInDatabase(recordedBy: User? = null): Result<TeamAliasDEO> {
    CacheUtil.deleteCachedTeamList()
    val deo = this
    return lockedTransaction {
        val team = Team.findById(deo.teamId)
            ?: return@lockedTransaction Result.failure(Exception("Team not found"))
        createTeamAliasInTransaction(team, deo.alias, LocalDate.now(), recordedBy)
            .map { it.toDEO() }
    }
}

suspend fun UpdateTeamAliasDEO.updateInDatabase(recordedBy: User? = null): Result<TeamAliasDEO> {
    CacheUtil.deleteCachedTeamList()
    val deo = this
    return lockedTransaction {
        val existing = TeamAlias.findById(deo.id)
            ?: return@lockedTransaction Result.failure(Exception("Alias not found"))
        validateAliasInTransaction(deo.alias, excludeAliasId = deo.id).map { normalizedValue ->
            val oldText = existing.alias
            val trimmed = deo.alias.trim()
            existing.alias = trimmed
            existing.normalized = normalizedValue
            writeTeamHistoryEventInTransaction(
                existing.team, TeamChangeType.ALIAS_ADDED, LocalDate.now(), oldText, trimmed, recordedBy
            )
            existing.toDEO()
        }
    }
}

suspend fun DeleteTeamAliasDEO.deleteFromDatabase(recordedBy: User? = null): Result<DeleteTeamAliasDEO> {
    CacheUtil.deleteCachedTeamList()
    val deo = this
    return lockedTransaction {
        val existing = TeamAlias.findById(deo.id)
            ?: return@lockedTransaction Result.failure(Exception("Alias not found"))
        writeTeamHistoryEventInTransaction(
            existing.team, TeamChangeType.ALIAS_REMOVED, LocalDate.now(), existing.alias, null, recordedBy
        )
        existing.delete()
        Result.success(DeleteTeamAliasDEO(deo.id))
    }
}

/**
 * Bulk alias lookup for the team list. One query for all teams, grouped by
 * team id, so building the list stays O(1) queries.
 * Requires an active transaction.
 */
fun aliasesForTeamsInTransaction(teamIds: List<Long>): Map<Long, List<TeamAliasDEO>> {
    if (teamIds.isEmpty()) {
        return emptyMap()
    }
    return TeamAliases.selectAll().where { TeamAliases.team inList teamIds }
        .map {
            TeamAliasDEO(
                id = it[TeamAliases.id].value,
                teamId = it[TeamAliases.team].value,
                alias = it[TeamAliases.alias]
            )
        }
        .groupBy { it.teamId }
}
