package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction


@Serializable
data class TeamDEO(
    val name: String, val id: Long, val isAmalgamation: Boolean, val amalgamationTeams: List<TeamDEO>?
) {
    companion object {
        fun fromTeam(input: Team, amalgamationTeams: List<TeamDEO>? = null): TeamDEO {
            return TeamDEO(input.name, input.id.value, input.isAmalgamation, amalgamationTeams)
        }

    }

    fun updateInDatabase(): Result<Team> {
        val thisTeam = this
        return transaction {
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
                    return@transaction Result.failure(Exception("Amalgamation teams not provided"))
                }
                Result.success(team)
            } else {
                Result.failure(Exception("Team not found"))
            }
        }
    }
}

@Serializable
data class NewTeamDEO(val name: String)

@Serializable
data class NewAmalgamationDEO(val name: String, val teams: List<TeamDEO>)

@Serializable
data class MergeTeamsDEO(val baseTeam: Long, val teamsToMerge: List<Long>) {
    fun updateInDatabase(): Result<Team> {

        return transaction {
            val team = Team.findById(baseTeam)
            if (team != null) {
                teamsToMerge.forEach { mergeTeamId ->
                    val mergeTeam = Team.findById(mergeTeamId)
                    if (mergeTeam != null) {
                        //First update all amalgamations to point to the base team
                        Amalgamation.find { Amalgamations.addedTeam eq mergeTeam.id }.forEach {
                            it.amalgamation = team
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
}
