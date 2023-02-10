package eu.gaelicgames.referee.data

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