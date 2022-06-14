package eu.gaelicgames.referee.resources

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/api")
class Api() {
    @Serializable
    @Resource("teams_available")
    class TeamsAvailable(val parent: Api = Api())

    @Serializable
    @Resource("new_team")
    class NewTeam(val parent: Api)

    @Serializable
    @Resource("new_amalgamation")
    class NewAmalgamation(val parent: Api)
}