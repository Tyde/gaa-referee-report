package eu.gaelicgames.referee.data

import kotlinx.serialization.Serializable

@Serializable
data class TeamDEO(
    val name: String,
    val id: Long,
    val isAmalgamation: Boolean,
    val amalgamationTeams: List<TeamDEO>?
) {
    companion object {
        fun createFromTeam(input: Team, amalgamationTeams: List<TeamDEO>? = null): TeamDEO {
            return TeamDEO(input.name, input.id.value, input.isAmalgamation, amalgamationTeams)
        }

    }
}

@Serializable
data class NewTeamDEO(val name: String)

@Serializable
data class NewAmalgamationDEO(val name: String, val teams: List<TeamDEO>)