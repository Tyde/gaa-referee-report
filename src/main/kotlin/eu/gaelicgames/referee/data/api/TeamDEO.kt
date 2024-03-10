package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.lockedTransaction
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*

fun TeamDEO.Companion.fromTeam(input: Team, amalgamationTeams: List<TeamDEO>? = null): TeamDEO {
    return TeamDEO(input.name, input.id.value, input.isAmalgamation, amalgamationTeams)
}

fun TeamDEO.Companion.wrapRow(row: ResultRow): TeamDEO {
    val isAmalgamation = row[Teams.isAmalgamation]
    val addedTeams = if (isAmalgamation) {
        Amalgamations.leftJoin(Teams, { addedTeam }, { Teams.id }).selectAll()
            .where { Amalgamations.amalgamation eq row[Teams.id] }.map {
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
                val dbTeams = mapJoinedResultsToTeamDEO(query.selectAll().toList(), alias)
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

suspend fun TeamDEO.updateInDatabase(): Result<Team> {

    CacheUtil.deleteCachedTeamList()

    val thisTeam = this
    return lockedTransaction {
        val team = Team.findById(thisTeam.id)
        if (team != null) {
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
            Result.success(team)
        } else {
            Result.failure(Exception("Team not found"))
        }
    }
}


suspend fun MergeTeamsDEO.updateInDatabase(): Result<Team> {
    runBlocking {
        CacheUtil.deleteCachedTeamList()
    }
    return lockedTransaction {
        val team = Team.findById(baseTeam)
        if (team != null) {
            teamsToMerge.forEach { mergeTeamId ->
                val mergeTeam = Team.findById(mergeTeamId)
                if (mergeTeam != null) {
                    //First update all amalgamations to point to the base team
                    Amalgamation.find { Amalgamations.addedTeam eq mergeTeam.id }.forEach {
                        it.addedTeam = team
                    }

                    if (mergeTeam.isAmalgamation) {
                        val teamAmalgamationIds =
                            Amalgamation.find { Amalgamations.amalgamation eq team.id }.map { teamAmalgamation ->
                                teamAmalgamation.addedTeam.id.value
                            }
                        Amalgamation.find { Amalgamations.amalgamation eq mergeTeam.id }
                            .forEach { mergeTeamAmalgamation ->
                                if (teamAmalgamationIds.contains(mergeTeamAmalgamation.addedTeam.id.value)) {
                                    mergeTeamAmalgamation.delete()
                                } else {
                                    mergeTeamAmalgamation.amalgamation = team
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

                    //Delete the team
                    mergeTeam.delete()
                }
            }

            Result.success(team)
        } else {
            Result.failure(Exception("Team not found"))
        }
    }
}

suspend fun NewAmalgamationDEO.createInDatabase(): Result<Team> {
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
                    return@lockedTransaction Result.failure(Exception("Team ${dbTeam.name} is already an amalgamation"))
                }
            }


        }

        return@lockedTransaction Result.success(Unit)
    }
    val newAmalgamationDB = prechecks.map {
        lockedTransaction {

            val amalgamation_base = Team.new {
                name = newAmalgamation.name
                isAmalgamation = true
            }
            for (team in newAmalgamation.teams) {
                Team.find { Teams.id eq team.id }.firstOrNull()?.let {
                    Amalgamation.new {
                        amalgamation = amalgamation_base
                        addedTeam = it
                    }
                }
            }
            amalgamation_base
        }
    }
    return newAmalgamationDB
}


