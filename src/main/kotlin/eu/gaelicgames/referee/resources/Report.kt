package eu.gaelicgames.referee.resources

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/report")
class Report() {

    @Serializable
    @Resource("new")
    class New(val parent:Report)

    @Serializable
    @Resource("edit/{id}")
    class Edit(val parent:Report, val id:Long){

        @Serializable
        @Resource("game/{gameId}")
        class Game(val parent:Edit, val gameId:Long)
    }
    @Serializable
    @Resource("show/{id}")
    class Show(val parent: Report, val id:Long)

    @Serializable
    @Resource("share/{uuid}")
    class Share(val parent: Report, val uuid:String)
}