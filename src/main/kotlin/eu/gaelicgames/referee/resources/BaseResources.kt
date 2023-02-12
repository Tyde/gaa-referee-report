package eu.gaelicgames.referee.resources

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/user")
class UserRes() {

    @Serializable
    @Resource("activate/{uuid}")
    class Activate(val parent: UserRes, val uuid: String)

}