package eu.gaelicgames.referee.data

import io.ktor.server.auth.*
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.*

object Users : LongIdTable() {
    val firstName = varchar("first_name",60)
    val lastName = varchar("last_name",60)
    val password = binary("password", 60)
    val mail = varchar("mail", 100)
}

class User(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<User>(Users)
    var firstName by Users.firstName
    var lastName by Users.lastName
    var password by Users.password
    var mail by Users.mail
}

object Sessions : LongIdTable() {
    val uuid = uuid("uuid")
    val user = reference("user", Users)
    val expires = datetime("expires")
}

class Session(id:EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Session> (Sessions)
    var uuid by Sessions.uuid
    var user by User referencedOn Sessions.user
    var expires by Sessions.expires
}

class UserPrincipal(val user:User) : Principal {
    override fun equals(other: Any?): Boolean {
        if (other is UserPrincipal) {
            return other.user == user
        }
        return false
    }
}

data class UserSession(val uuid: UUID) : Principal

