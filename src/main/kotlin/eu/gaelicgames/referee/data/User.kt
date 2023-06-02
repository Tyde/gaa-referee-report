package eu.gaelicgames.referee.data

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.server.auth.*
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.*
enum class UserRole {
    ADMIN,
    REFEREE,
    INACTIVE,
    WAITING_FOR_ACTIVATION
}

object Users : LongIdTable() {
    val firstName = varchar("first_name",60)
    val lastName = varchar("last_name",60)
    val password = binary("password", 60)
    val mail = varchar("mail", 100)
    val role = enumeration("role", UserRole::class)
}

class User(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<User>(Users) {
        fun newWithPassword(
            firstName: String,
            lastName: String,
            mail: String,
            password: String,
            role: UserRole
        ):User {
            return User.new {
                this.firstName = firstName
                this.lastName = lastName
                this.mail = mail
                this.password = hashPassword(password)
                this.role = role
            }
        }

        fun hashPassword(password: String): ByteArray {
            return BCrypt
                .withDefaults().hash(12, password.toCharArray())
        }


    }
    var firstName by Users.firstName
    var lastName by Users.lastName
    var password by Users.password
    var mail by Users.mail
    var role by Users.role

    fun verifyPassword(password: String): Boolean {

        return BCrypt.verifyer().verify(password.toCharArray(), this.password).verified
    }

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

object ActivationTokens : LongIdTable() {
    val token = uuid("token")
    val user = reference("user", Users)
    val expires = datetime("expires")


}

class ActivationToken(id:EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ActivationToken> (ActivationTokens)
    var token by ActivationTokens.token
    var user by User referencedOn ActivationTokens.user
    var expires by ActivationTokens.expires
}

