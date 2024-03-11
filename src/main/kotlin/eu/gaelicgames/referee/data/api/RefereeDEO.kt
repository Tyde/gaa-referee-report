package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.GGERefereeConfig
import eu.gaelicgames.referee.util.MailjetClientHandler
import eu.gaelicgames.referee.util.lockedTransaction
import org.jetbrains.exposed.sql.ResultRow
import java.io.IOException
import java.time.LocalDateTime
import java.util.*

fun RefereeDEO.Companion.fromReferee(referee: User): RefereeDEO {
    return RefereeDEO(
        id = referee.id.value,
        firstName = referee.firstName,
        lastName = referee.lastName,
        mail = referee.mail,
    )
}

fun RefereeDEO.Companion.wrapRow(row: ResultRow): RefereeDEO {
    val id = row[Users.id].value
    val firstName = row[Users.firstName]
    val lastName = row[Users.lastName]
    val mail = row[Users.mail]
    return RefereeDEO(id, firstName, lastName, mail)
}
suspend fun RefereeWithRoleDEO.updateInDatabase():Result<User> {
    val thisReferee = this
    return lockedTransaction {
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

fun generateActivationLink(referee: User): String {
    val activationUUID = UUID.randomUUID()
    ActivationToken.new {
        this.token = activationUUID
        this.user = referee
        this.expires = LocalDateTime.now().plusDays(30)
    }

    val activationString = activationUUID.toString()
    return GGERefereeConfig.serverUrl + "/user/activate/$activationString"
}

suspend fun NewRefereeDEO.createInDatabase(): Result<User> {
    val thisReferee = this
    return lockedTransaction {
        val extractedRole = if (thisReferee.isReferee) {
            UserRole.WAITING_FOR_ACTIVATION
        } else {
            UserRole.CCC_WAITING_FOR_ACTIVATION
        }
        val referee = User.new {
            firstName = thisReferee.firstName
            lastName = thisReferee.lastName
            mail = thisReferee.mail
            password = "".toByteArray()
            role = extractedRole
        }

        val activationLink = generateActivationLink(referee)
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

suspend fun TokenDEO.validate(): Result<User> {
    val thisToken = this
    return lockedTransaction {
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


suspend fun NewPasswordByTokenDEO.updatePassword(): Result<User> {
    val thisToken = this
    return lockedTransaction {
        val uuid = kotlin.runCatching { UUID.fromString(thisToken.token) }
        if (uuid.isSuccess) {
            val token = ActivationToken.find { ActivationTokens.token eq uuid.getOrThrow() }
                .firstOrNull()
            if (token != null) {

                if (token.expires.isAfter(LocalDateTime.now())) {
                    val expectedRole = when (token.user.role) {
                        UserRole.WAITING_FOR_ACTIVATION -> {
                            UserRole.REFEREE
                        }
                        UserRole.CCC_WAITING_FOR_ACTIVATION -> {
                            UserRole.CCC
                        }
                        else -> {
                            token.user.role //Password reset
                        }
                    }
                    println("Expected role: $expectedRole")
                    val user = token.user
                    user.password = User.hashPassword(thisToken.password)
                    user.role = expectedRole
                    token.delete()
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


suspend fun LoginDEO.validate(): Result<User> {
    val thisLogin = this
    return lockedTransaction {
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

fun RefereeWithRoleDEO.Companion.fromUser(user: User): RefereeWithRoleDEO {
    return RefereeWithRoleDEO(
        id = user.id.value,
        firstName = user.firstName,
        lastName = user.lastName,
        mail = user.mail,
        role = user.role
    )
}

suspend fun UpdateRefereeDAO.updateInDatabase(): Result<User> {
    val thisReferee = this
    return lockedTransaction {
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



suspend fun UpdateRefereePasswordDAO.updateInDatabase(): Result<UpdateRefereePasswordResponse> {
    val thisReferee = this
    return lockedTransaction {
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


suspend fun SetRefereeRole.updateInDatabase(): Result<User> {
    val thisReferee = this
    return lockedTransaction {
        val referee = User.findById(thisReferee.id)
        if (referee != null) {
            referee.role = thisReferee.role
            Result.success(referee)
        } else {
            Result.failure(IllegalArgumentException("Trying to update a referee with invalid id ${thisReferee.id}"))
        }
    }
}


suspend fun ResetRefereePasswordDEO.executePasswordReset(): Result<UpdateRefereePasswordResponse> {
    val refereeID = this.id
    return lockedTransaction {
        val referee = User.findById(refereeID)
        if (referee == null) {
            return@lockedTransaction Result.failure(IllegalArgumentException("Referee with id $refereeID not found"))
        }
        val activationLink = generateActivationLink(referee)

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





