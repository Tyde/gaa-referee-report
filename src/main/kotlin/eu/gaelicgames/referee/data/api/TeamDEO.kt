package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.lockedTransaction
import org.jetbrains.exposed.sql.*
import java.time.LocalDate
import java.time.LocalDateTime

// ---------------------------------------------------------------------------
// Team history helpers
// ---------------------------------------------------------------------------

/**
 * Writes a history event. Requires an active transaction.
 * Used inside existing `lockedTransaction { }` blocks so history is written
 * atomically with the change itself.
 */
fun writeTeamHistoryEventInTransaction(
    team: Team,
    changeType: TeamChangeType,
    changeDate: LocalDate,
    oldValue: String? = null,
    newValue: String? = null,
    recordedBy: User? = null
) {
    TeamHistoryEvent.new {
        this.team = team
        this.changeType = changeType
        this.changeDate = changeDate
        this.oldValue = oldValue
        this.newValue = newValue
        recordedAt = LocalDateTime.now()
        this.recordedBy = recordedBy
    }
}

fun TeamHistoryEvent.toDEO(): TeamHistoryEventDEO {
    return TeamHistoryEventDEO(
        changeType = changeType.name,
        changeDate = changeDate,
        oldValue = oldValue,
        newValue = newValue,
        recordedAt = recordedAt
    )
}

suspend fun TeamDEO.Companion.historyForTeam(teamId: Long): List<TeamHistoryEventDEO> {
    return lockedTransaction {
        TeamHistoryEvent.find { TeamHistoryEvents.team eq teamId }
            .sortedBy { it.changeDate }
            .map { it.toDEO() }
    }
}

// ---------------------------------------------------------------------------
// Team DEO mapping
// ---------------------------------------------------------------------------

fun TeamDEO.Companion.fromTeam(input: Team, amalgamationTeams: List<TeamDEO>? = null): TeamDEO {
    return TeamDEO(input.name, input.id.value, input.isAmalgamation, amalgamationTeams)
}

fun TeamDEO.Companion.wrapRow(row: ResultRow): TeamDEO {
    val isAmalgamation = row[Teams.isAmalgamation]
    val addedTeams = if (isAmalgamation) {
        Amalgamations.leftJoin(Teams, { addedTeam }, { Teams.id }).selectAll()
            .where { (Amalgamations.amalgamation eq row[Teams.id]) and (Teams.deletedAt.isNull()) }.map {
            TeamDEO.wrapRow(it)
        }

    } else {
        listOf()
    }
    return TeamDEO(
        row[Teams.name],
        row[Teams.id].value,
        isAmalgamation,
        addedTeams
    )
}

fun TeamDEO.Companion.wrapJoinedRow(row: ResultRow, aliasAddedTeam: Alias<Teams>): TeamDEO {
    val isAmalgamation = row[Teams.isAmalgamation]
    val singleAmalgamationTeam = if (isAmalgamation) {
        val deleted = row[aliasAddedTeam[Teams.deletedAt]]
        if (deleted == null) {
            listOf(
                TeamDEO(
                    row[aliasAddedTeam[Teams.name]],
                    row[aliasAddedTeam[Teams.id]].value,
                    row[aliasAddedTeam[Teams.isAmalgamation]],
                    null
                )
            )
        } else {
            listOf()
        }
    } else {
        listOf()
    }
    return TeamDEO(
        row[Teams.name],
        row[Teams.id].value,
        isAmalgamation,
        singleAmalgamationTeam
    )
}

fun TeamDEO.Companion.wrapJoinQuery(): Pair<Join, Alias<Teams>> {
    val addedTeamAlias = Teams.alias("addedTeam")
    val q = Teams
        .leftJoin(Amalgamations, { Teams.id }, { Amalgamations.amalgamation })
        .leftJoin(addedTeamAlias, { Amalgamations.addedTeam }, { addedTeamAlias[Teams.id] })
    return Pair(q, addedTeamAlias)
}

fun TeamDEO.Companion.mapJoinedResultsToTeamDEO(
    results: List<ResultRow>,
    aliasAddedTeam: Alias<Teams>
): List<TeamDEO> {
    return results
        .map {wrapJoinedRow(it, aliasAddedTeam)}
        .groupBy { it.id }.map { (_, teams) ->
            val template = teams.first()
            TeamDEO(
                name = template.name,
                id = template.id,
                isAmalgamation = template.isAmalgamation,
                amalgamationTeams = teams.flatMap { it.amalgamationTeams ?: listOf() }
            )
        }

}

suspend fun TeamDEO.Companion.allTeamList(): List<TeamDEO> {
    return CacheUtil.getCachedTeamList()
        .getOrElse {
            lockedTransaction {
                val (query, alias) = wrapJoinQuery()
                val dbTeams = mapJoinedResultsToTeamDEO(
                    query.selectAll().where { Teams.deletedAt.isNull() }.toList(),
                    alias
                )
                CacheUtil.cacheTeamList(dbTeams)
                dbTeams
            }
        }

}

suspend fun TeamDEO.Companion.fromTeamId(it: Long): Result<TeamDEO> {
    return lockedTransaction {
        val teamDEO = Team.findById(it)?.let { fromTeam(it) }
        teamDEO?.let { Result.success(it) } ?: Result.failure(Exception("Team not found"))
    }
}

// ---------------------------------------------------------------------------
// Team mutations (with history recording)
// ---------------------------------------------------------------------------

suspend fun TeamDEO.updateInDatabase(recordedBy: User? = null): Result<Team> {

    CacheUtil.deleteCachedTeamList()

    val thisTeam = this
    val changeDate = thisTeam.changeDate ?: LocalDate.now()
    return lockedTransaction {
        val team = Team.findById(thisTeam.id)
        if (team != null) {
            val oldName = team.name
            val oldIsAmalgamation = team.isAmalgamation
            val oldMemberIds = Amalgamation.find { Amalgamations.amalgamation eq team.id }
                .map { it.addedTeam.id.value }.toSet()

            team.name = thisTeam.name
            team.isAmalgamation = thisTeam.isAmalgamation
            if (thisTeam.isAmalgamation && thisTeam.amalgamationTeams != null) {
                thisTeam.amalgamationTeams.forEach { addedTeam ->
                    val connection = Amalgamation.find {
                        Amalgamations.amalgamation eq team.id and (Amalgamations.addedTeam eq addedTeam.id)
                    }.firstOrNull()
                    if (connection == null) {
                        val addedDBTeam = Team.findById(addedTeam.id)
                        if (addedDBTeam != null) {
                            Amalgamation.new {
                                amalgamation = team
                                this.addedTeam = addedDBTeam
                            }
                        }
                    }
                }
                Amalgamation.find {
                    Amalgamations.amalgamation eq team.id and
                            (Amalgamations.addedTeam.notInList(thisTeam.amalgamationTeams.map { it.id }))
                }.forEach { it.delete() }
            } else if (thisTeam.isAmalgamation && thisTeam.amalgamationTeams == null) {
                return@lockedTransaction Result.failure(Exception("Amalgamation teams not provided"))
            }

            val newMemberIds = Amalgamation.find { Amalgamations.amalgamation eq team.id }
                .map { it.addedTeam.id.value }.toSet()

            // ---- history ----
            if (oldName != team.name) {
                writeTeamHistoryEventInTransaction(
                    team, TeamChangeType.RENAMED, changeDate, oldName, team.name, recordedBy
                )
            }
            if (oldIsAmalgamation != team.isAmalgamation) {
                val changeType = if (team.isAmalgamation) {
                    TeamChangeType.CONVERTED_TO_AMALGAMATION
                } else {
                    TeamChangeType.CONVERTED_TO_TEAM
                }
                writeTeamHistoryEventInTransaction(team, changeType, changeDate, null, null, recordedBy)
            }
            (newMemberIds - oldMemberIds).forEach { memberId ->
                val memberName = Team.findById(memberId)?.name ?: memberId.toString()
                writeTeamHistoryEventInTransaction(
                    team, TeamChangeType.MEMBER_ADDED, changeDate, null, memberName, recordedBy
                )
            }
            (oldMemberIds - newMemberIds).forEach { memberId ->
                val memberName = Team.findById(memberId)?.name ?: memberId.toString()
                writeTeamHistoryEventInTransaction(
                    team, TeamChangeType.MEMBER_REMOVED, changeDate, memberName, null, recordedBy
                )
            }

            Result.success(team)
        } else {
            Result.failure(Exception("Team not found"))
        }
    }
}


suspend fun MergeTeamsDEO.updateInDatabase(recordedBy: User? = null): Result<Team> {

    CacheUtil.deleteCachedTeamList()

    val changeDate = this.changeDate ?: LocalDate.now()
    return lockedTransaction {
        val team = Team.findById(baseTeam)
        if (team != null) {
            teamsToMerge.forEach { mergeTeamId ->
                val mergeTeam = Team.findById(mergeTeamId)
                if (mergeTeam != null) {
                    //Capture state before mutation for history purposes
                    val amalgamationsContainingMergeTeam = Amalgamation.find {
                        Amalgamations.addedTeam eq mergeTeam.id
                    }.toList()
                    val mergeTeamAmalgamationMembers = if (mergeTeam.isAmalgamation) {
                        Amalgamation.find { Amalgamations.amalgamation eq mergeTeam.id }.toList()
                    } else {
                        listOf()
                    }

                    //First update all amalgamations to point to the base team
                    amalgamationsContainingMergeTeam.forEach { amalgamationEntry ->
                        val baseTeamAlreadyMember = Amalgamation.find {
                            (Amalgamations.amalgamation eq amalgamationEntry.amalgamation.id) and
                                    (Amalgamations.addedTeam eq team.id)
                        }.count() > 0
                        if (baseTeamAlreadyMember) {
                            //The amalgamation already contains the base team -> the merged team
                            // just disappears from it (avoids duplicate members)
                            writeTeamHistoryEventInTransaction(
                                amalgamationEntry.amalgamation, TeamChangeType.MEMBER_REMOVED, changeDate,
                                mergeTeam.name, null, recordedBy
                            )
                            amalgamationEntry.delete()
                        } else {
                            amalgamationEntry.addedTeam = team
                            writeTeamHistoryEventInTransaction(
                                amalgamationEntry.amalgamation, TeamChangeType.MEMBER_ADDED, changeDate,
                                mergeTeam.name, team.name, recordedBy
                            )
                        }
                    }

                    if (mergeTeam.isAmalgamation) {
                        val teamAmalgamationIds =
                            Amalgamation.find { Amalgamations.amalgamation eq team.id }.map { teamAmalgamation ->
                                teamAmalgamation.addedTeam.id.value
                            }
                        mergeTeamAmalgamationMembers.forEach { mergeTeamAmalgamation ->
                            if (teamAmalgamationIds.contains(mergeTeamAmalgamation.addedTeam.id.value)) {
                                mergeTeamAmalgamation.delete()
                            } else {
                                mergeTeamAmalgamation.amalgamation = team
                                writeTeamHistoryEventInTransaction(
                                    team, TeamChangeType.MEMBER_ADDED, changeDate,
                                    mergeTeam.name, mergeTeamAmalgamation.addedTeam.name, recordedBy
                                )
                            }
                        }
                    }

                    //Update all DisciplinaryActions
                    DisciplinaryAction.find { DisciplinaryActions.team eq mergeTeam.id }.forEach {
                        it.team = team
                    }

                    //Update all GameReports
                    GameReport.find { GameReports.teamA eq mergeTeam.id }.forEach {
                        it.teamA = team
                    }
                    GameReport.find { GameReports.teamB eq mergeTeam.id }.forEach {
                        it.teamB = team
                    }

                    //Update all Injuries
                    Injury.find { Injuries.team eq mergeTeam.id }.forEach {
                        it.team = team
                    }

                    //Update all TournamentReportTeamPreSelections
                    TournamentReportTeamPreSelection.find {
                        TournamentReportTeamPreSelections.team eq mergeTeam.id
                    }.forEach {
                        //Avoid duplicates
                        val bothTeamsInSameReport = TournamentReportTeamPreSelection.find {
                            TournamentReportTeamPreSelections.report eq it.report.id and
                                    (TournamentReportTeamPreSelections.team eq team.id)
                        }.count() > 0
                        if (!bothTeamsInSameReport) {
                            it.team = team
                        } else {
                            it.delete()
                        }
                    }

                    //Update all TournamentTeamPreSelections
                    TournamentTeamPreSelection.find {
                        TournamentTeamPreSelections.team eq mergeTeam.id
                    }.forEach {
                        //Avoid duplicates
                        val bothTeamsInSameTournament = TournamentTeamPreSelection.find {
                            TournamentTeamPreSelections.tournament eq it.tournament.id and
                                    (TournamentTeamPreSelections.team eq team.id)
                        }.count() > 0
                        if (!bothTeamsInSameTournament) {
                            it.team = team
                        } else {
                            it.delete()
                        }
                    }

                    //History + soft-delete instead of hard delete
                    writeTeamHistoryEventInTransaction(
                        mergeTeam, TeamChangeType.MERGED_INTO, changeDate,
                        mergeTeam.name, team.name, recordedBy
                    )
                    mergeTeam.deletedAt = LocalDateTime.now()
                    mergeTeam.mergedInto = team
                }
            }

            Result.success(team)
        } else {
            Result.failure(Exception("Team not found"))
        }
    }
}

suspend fun NewAmalgamationDEO.createInDatabase(recordedBy: User? = null): Result<Team> {
    val newAmalgamation = this
    CacheUtil.deleteCachedTeamList()
    val prechecks = lockedTransaction {
        //First check if any of the teams is actually an amalgamation
        for (team in newAmalgamation.teams) {
            val dbTeam = Team.find { Teams.id eq team.id }.firstOrNull()
            if (dbTeam == null) {
                return@lockedTransaction Result.failure(Exception("Team ${team.name} (id=${team.id}) not found"))
            } else {
                if (dbTeam.isAmalgamation) {
                    val subAmalgamatedTeamsCount = Amalgamation.find { Amalgamations.amalgamation eq dbTeam.id }.count()
                    if (subAmalgamatedTeamsCount > 1) {
                        // We will allow squads to be amalgamated, but not amalgamations to be amalgamated
                        return@lockedTransaction Result.failure(Exception("Team ${dbTeam.name} is already an amalgamation"))

                    }
                }
            }


        }

        //Then check if the amalgamation name is unique
        val amalgamation = Team.find { Teams.name eq newAmalgamation.name }.firstOrNull()
        if (amalgamation != null) {
            return@lockedTransaction Result.failure(Exception("Amalgamation/Team name ${newAmalgamation.name} already exists"))
        }

        return@lockedTransaction Result.success(Unit)
    }
    val newAmalgamationDB = prechecks.map {
        lockedTransaction {
            val changeDate = newAmalgamation.changeDate ?: LocalDate.now()

            val amalgamation_base = Team.new {
                name = newAmalgamation.name
                isAmalgamation = true
            }
            writeTeamHistoryEventInTransaction(
                amalgamation_base, TeamChangeType.CREATED, changeDate,
                null, amalgamation_base.name, recordedBy
            )
            for (team in newAmalgamation.teams) {
                Team.find { Teams.id eq team.id }.firstOrNull()?.let {
                    Amalgamation.new {
                        amalgamation = amalgamation_base
                        addedTeam = it
                    }
                    writeTeamHistoryEventInTransaction(
                        amalgamation_base, TeamChangeType.MEMBER_ADDED, changeDate,
                        null, it.name, recordedBy
                    )
                }
            }
            amalgamation_base
        }
    }
    return newAmalgamationDB
}
