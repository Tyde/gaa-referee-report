package eu.gaelicgames.referee.data

import eu.gaelicgames.referee.util.GGERefereeConfig
import eu.gaelicgames.referee.util.MailjetClientHandler
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


object LocalDateSerializer : KSerializer<LocalDate> {
    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(DateTimeFormatter.ISO_DATE.format(value))
    }

}

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(decoder.decodeString()))
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(DateTimeFormatter.ISO_DATE_TIME.format(value))
    }

}

@Serializable
data class GameCodeDEO(
    val name: String, val id: Long
) {
    constructor(gameCode: GameCode) : this(gameCode.name, gameCode.id.value)

}


@Serializable
enum class ApiErrorOptions {
    @SerialName("insertionFailed")
    INSERTION_FAILED,

    @SerialName("deleteFailed")
    DELETE_FAILED,

    @SerialName("notFound")
    NOT_FOUND,

    @SerialName("notAuthorized")
    NOT_AUTHORIZED
}

@Serializable
data class ApiError(
    val error: ApiErrorOptions, val message: String
)


@Serializable
data class RefereeDEO(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val mail: String,
) {
    companion object {
        fun fromReferee(referee: User): RefereeDEO {
            return RefereeDEO(
                id = referee.id.value,
                firstName = referee.firstName,
                lastName = referee.lastName,
                mail = referee.mail,
            )
        }
    }

    fun updateInDatabase():Result<User> {
        val thisReferee = this
        return transaction {
            val referee = User.findById(thisReferee.id)
            if(referee != null) {
                referee.firstName = thisReferee.firstName
                referee.lastName = thisReferee.lastName
                referee.mail = thisReferee.mail
                Result.success(referee)
            } else {
                Result.failure(IllegalArgumentException("Trying to update a referee with invalid id ${thisReferee.id}"))
            }
        }
    }
}

@Serializable
data class NewRefereeDEO(
    val firstName: String,
    val lastName: String,
    val mail: String
) {
    fun createInDatabase(): Result<User> {
        val thisReferee = this
        return transaction {
            val referee = User.new {
                firstName = thisReferee.firstName
                lastName = thisReferee.lastName
                mail = thisReferee.mail
                password = "".toByteArray()
                role = UserRole.WAITING_FOR_ACTIVATION
            }
            val activationUUID = UUID.randomUUID()
            ActivationToken.new {
                this.token = activationUUID
                this.user = referee
                this.expires = LocalDateTime.now().plusDays(7)
            }

            val activationString = activationUUID.toString()
            val activationLink = GGERefereeConfig.serverUrl+"/user/activate/$activationString"
            println(activationLink)
            val name = "${referee.firstName} ${referee.lastName}"
            kotlin.runCatching {  MailjetClientHandler.sendActivationMail(
                name,
                activationLink,
                referee.mail,
            )}.onFailure {
                println("Failed to send activation mail to $name")
                it.printStackTrace()
            }.map { referee }
        }
    }
}

@Serializable
data class TokenDEO(
    val token: String
) {
    fun validate(): Result<User> {
        val thisToken = this
        return transaction {
            val uuid = kotlin.runCatching { UUID.fromString(thisToken.token) }
            if (uuid.isSuccess) {
                val token = ActivationToken.find { ActivationTokens.token eq uuid.getOrThrow() }
                    .firstOrNull()
                if (token != null) {
                    if (token.expires.isAfter(LocalDateTime.now())) {
                        val user = token.user
                        Result.success(user)
                    } else {
                        Result.failure(IllegalArgumentException("Token expired"))
                    }
                } else {
                    Result.failure(IllegalArgumentException("Token not found"))
                }
            } else {
                Result.failure(uuid.exceptionOrNull() ?: IllegalArgumentException("Token not valid"))
            }
        }
    }
}

@Serializable
data class NewPasswordByTokenDEO(
    val token: String,
    val password: String
) {
    fun updatePassword(): Result<User> {
        val thisToken = this
        return transaction {
            val uuid = kotlin.runCatching { UUID.fromString(thisToken.token) }
            if (uuid.isSuccess) {
                val token = ActivationToken.find { ActivationTokens.token eq uuid.getOrThrow() }
                    .firstOrNull()
                if (token != null) {
                    if (token.expires.isAfter(LocalDateTime.now())) {
                        val user = token.user
                        user.password = User.hashPassword(thisToken.password)
                        user.role = UserRole.REFEREE
                        Result.success(user)
                    } else {
                        Result.failure(IllegalArgumentException("Token expired"))
                    }
                } else {
                    Result.failure(IllegalArgumentException("Token not found"))
                }
            } else {
                Result.failure(uuid.exceptionOrNull() ?: IllegalArgumentException("Token not valid"))
            }
        }
    }
}

@Serializable
data class LoginDEO(
    val mail: String,
    val password: String
) {
    fun validate(): Result<User> {
        val thisLogin = this
        return transaction {
            val user = User.find { Users.mail eq thisLogin.mail }.firstOrNull()
            if (user != null) {

                if (user.verifyPassword(thisLogin.password)) {
                    Result.success(user)
                } else {
                    Result.failure(IllegalArgumentException("Password not correct"))
                }
            } else {
                Result.failure(IllegalArgumentException("User not found"))
            }
        }
    }
}

@Serializable
data class SessionInfoDEO(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val mail: String,
    val role: UserRole
) {
    companion object {
        fun fromUser(user: User): SessionInfoDEO {
            return SessionInfoDEO(
                id = user.id.value,
                firstName = user.firstName,
                lastName = user.lastName,
                mail = user.mail,
                role = user.role
            )
        }
    }
}
