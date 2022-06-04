package eu.gaelicgames.referee.resources

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/report")
class Report {
    @Serializable
    @Resource("new")
    class New()

    @Serializable
    @Resource("edit/{id}")
    class Edit(val id:Long){

        @Serializable
        @Resource("game/{gameId}")
        class Game(val parent:Edit, val gameId:Long)
    }

}