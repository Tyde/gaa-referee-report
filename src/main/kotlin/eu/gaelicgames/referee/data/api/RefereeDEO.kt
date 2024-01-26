package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.GGERefereeConfig
import eu.gaelicgames.referee.util.MailjetClientHandler
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.IOException
import java.time.LocalDateTime
import java.util.*

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
            if (referee != null) {
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
            val activationLink = GGERefereeConfig.serverUrl + "/user/activate/$activationString"
            println(activationLink)
            val name = "${referee.firstName} ${referee.lastName}"
            kotlin.runCatching {
                MailjetClientHandler.sendActivationMail(
                    name,
                    activationLink,
                    referee.mail,
                )
            }.onFailure {
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
data class RefereeWithRoleDEO(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val mail: String,
    val role: UserRole
) {
    companion object {
        fun fromUser(user: User): RefereeWithRoleDEO {
            return RefereeWithRoleDEO(
                id = user.id.value,
                firstName = user.firstName,
                lastName = user.lastName,
                mail = user.mail,
                role = user.role
            )
        }
    }
}

@Serializable
data class UpdateRefereeDAO(
    val id: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val mail: String? = null
) {
    fun updateInDatabase(): Result<User> {
        val thisReferee = this
        return transaction {
            val referee = User.findById(thisReferee.id)
            if (referee != null) {
                if (thisReferee.firstName != null) {
                    referee.firstName = thisReferee.firstName
                }
                if (thisReferee.lastName != null) {
                    referee.lastName = thisReferee.lastName
                }
                if (thisReferee.mail != null) {
                    referee.mail = thisReferee.mail
                }
                Result.success(referee)
            } else {
                Result.failure(IllegalArgumentException("Trying to update a referee with invalid id ${thisReferee.id}"))
            }
        }
    }
}

@Serializable
data class UpdateRefereePasswordResponse(
    val id: Long,
    val success: Boolean,
    val message: String? = null
)

@Serializable
data class UpdateRefereePasswordDAO(
    val id: Long,
    val oldPassword: String,
    val newPassword: String
) {
    fun updateInDatabase(): Result<UpdateRefereePasswordResponse> {
        val thisReferee = this
        return transaction {
            val referee = User.findById(thisReferee.id)
            if (referee != null) {
                if (referee.verifyPassword(thisReferee.oldPassword)) {
                    referee.password = User.hashPassword(thisReferee.newPassword)
                    Result.success(UpdateRefereePasswordResponse(thisReferee.id, true))
                } else {
                    Result.success(UpdateRefereePasswordResponse(thisReferee.id, false, "Old password not correct"))
                }
            } else {
                Result.failure(IllegalArgumentException("Trying to update a referee with invalid id ${thisReferee.id}"))
            }
        }
    }
}


@Serializable
data class SetRefereeRole(
    val id: Long,
    val role: UserRole
) {
    fun updateInDatabase(): Result<User> {
        val thisReferee = this
        return transaction {
            val referee = User.findById(thisReferee.id)
            if (referee != null) {
                referee.role = thisReferee.role
                Result.success(referee)
            } else {
                Result.failure(IllegalArgumentException("Trying to update a referee with invalid id ${thisReferee.id}"))
            }
        }
    }
}


@Serializable
data class ResetRefereePasswordDEO(
    val id: Long
) {
    fun executePasswordReset(): Result<UpdateRefereePasswordResponse> {
        val refereeID = this.id
        return transaction {
            val referee = User.findById(refereeID)
            if (referee == null) {
                return@transaction Result.failure(IllegalArgumentException("Referee with id $refereeID not found"))
            }
            val activationUUID = UUID.randomUUID()
            ActivationToken.new {
                this.token = activationUUID
                this.user = referee
                this.expires = LocalDateTime.now().plusDays(7)
            }

            val activationString = activationUUID.toString()
            val activationLink = GGERefereeConfig.serverUrl + "/user/activate/$activationString"
            val name = referee.firstName + " " + referee.lastName
            kotlin.runCatching {
                MailjetClientHandler.sendPasswordResetMail(
                    name,
                    activationLink,
                    referee.mail,
                )
            }.fold(
                onSuccess = {
                    Result.success(UpdateRefereePasswordResponse(referee.id.value, true))
                },
                onFailure = {
                    Result.failure(IOException("Failed to send password reset mail to $name"))
                }
            )
        }
    }
}
