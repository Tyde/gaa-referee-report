package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.Amalgamation
import eu.gaelicgames.referee.data.Amalgamations
import eu.gaelicgames.referee.data.Team
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