package eu.gaelicgames.referee.data

import at.favre.lib.crypto.bcrypt.BCrypt
import eu.gaelicgames.referee.data.api.RefereeWithRoleDEO
import eu.gaelicgames.referee.data.api.fromUser
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.lockedTransaction
import io.ktor.server.auth.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*



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
    companion object : LongEntityClass<Session> (Sessions) {
        suspend fun validateSession(uuid: UUID): Result<SessionWithUserData> {
            val cachedSession = CacheUtil.getCachedSession(uuid)
            if(cachedSession.isSuccess) {
                println("Successful cache hit")
                return cachedSession
            } else {
                println("Cache miss: ${cachedSession.exceptionOrNull()?.message}")
            }

            return lockedTransaction {
                val foundSessions = Session.find { Sessions.uuid eq uuid }
                if (!foundSessions.empty() &&
                    foundSessions.first().expires > LocalDateTime.now()) {
                    val session = SessionWithUserData.fromSession(foundSessions.first())
                    if(foundSessions.first().expires < LocalDateTime.now().plusHours(6)) {
                        foundSessions.first().expires = LocalDateTime.now().plusDays(1)
                    }
                    CacheUtil.cacheSession(session)
                    Result.success(session)
                } else {
                    Result.failure(Exception("Session not found"))
                }
            }
        }
    }
    var uuid by Sessions.uuid
    var user by User referencedOn Sessions.user
    var expires by Sessions.expires


}

@Serializable
data class SessionWithUserData(
    val uuid: String,
    val user: RefereeWithRoleDEO,
    @Serializable(with = LocalDateTimeCacheSerializer::class)
    val expires: LocalDateTime
) {
    companion object {
        suspend fun fromSession(session: Session): SessionWithUserData {
            return lockedTransaction {
                SessionWithUserData(
                    session.uuid.toString(),
                    RefereeWithRoleDEO.fromUser(session.user),
                    session.expires
                )
            }
        }
    }
}

class UserPrincipal(val user:User) : Principal {
    override fun equals(other: Any?): Boolean {
        if (other is UserPrincipal) {
            return other.user == user
        }
        return false
    }

    override fun hashCode(): Int {
        return user.hashCode()
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

