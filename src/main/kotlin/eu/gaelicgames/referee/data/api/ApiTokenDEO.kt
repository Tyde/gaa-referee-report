package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.ApiToken
import eu.gaelicgames.referee.data.ApiTokens
import eu.gaelicgames.referee.data.User
import eu.gaelicgames.referee.data.UserRole
import eu.gaelicgames.referee.util.lockedTransaction
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64

@Serializable
data class NewApiTokenDEO(
    val name: String,
    val expiresInDays: Int? = null
)

@Serializable
data class ApiTokenCreatedDEO(
    val id: Long,
    val name: String,
    val token: String,
    val expiresAt: String?
)

@Serializable
data class ApiTokenDEO(
    val id: Long,
    val name: String,
    val createdAt: String,
    val expiresAt: String?,
    val revoked: Boolean,
    val lastUsedAt: String?
) {
    companion object {
        fun fromApiToken(token: ApiToken): ApiTokenDEO {
            return ApiTokenDEO(
                id = token.id.value,
                name = token.name,
                createdAt = token.createdAt.toString(),
                expiresAt = token.expiresAt?.toString(),
                revoked = token.revoked,
                lastUsedAt = token.lastUsedAt?.toString()
            )
        }
    }
}

@Serializable
data class RevokeApiTokenDEO(
    val id: Long
)

private fun generateApiToken(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return "gge_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

private fun sha256Hex(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

suspend fun NewApiTokenDEO.createInDatabase(admin: User): Result<ApiTokenCreatedDEO> {
    val thisDeo = this
    return lockedTransaction {
        val rawToken = generateApiToken()
        val now = LocalDateTime.now()
        val token = ApiToken.new {
            name = thisDeo.name
            tokenHash = sha256Hex(rawToken)
            user = admin
            createdAt = now
            expiresAt = thisDeo.expiresInDays?.let { now.plusDays(it.toLong()) }
        }
        Result.success(
            ApiTokenCreatedDEO(
                id = token.id.value,
                name = token.name,
                token = rawToken,
                expiresAt = token.expiresAt?.toString()
            )
        )
    }
}

suspend fun RevokeApiTokenDEO.revoke(): Result<ApiTokenDEO> {
    val tokenId = this.id
    return lockedTransaction {
        val token = ApiToken.findById(tokenId)
        if (token == null) {
            Result.failure(IllegalArgumentException("Api token with id $tokenId not found"))
        } else {
            token.revoked = true
            Result.success(ApiTokenDEO.fromApiToken(token))
        }
    }
}

suspend fun apiTokenList(): List<ApiTokenDEO> {
    return lockedTransaction {
        ApiToken.all().map { ApiTokenDEO.fromApiToken(it) }
    }
}

suspend fun validateApiToken(rawToken: String): Result<User> {
    return lockedTransaction {
        val hash = sha256Hex(rawToken)
        val token = ApiToken.find { ApiTokens.tokenHash eq hash }.firstOrNull()
        if (token == null) {
            return@lockedTransaction Result.failure(IllegalArgumentException("Api token not found"))
        }
        if (token.revoked) {
            return@lockedTransaction Result.failure(IllegalArgumentException("Api token is revoked"))
        }
        val now = LocalDateTime.now()
        val expiresAt = token.expiresAt
        if (expiresAt != null && expiresAt.isBefore(now)) {
            return@lockedTransaction Result.failure(IllegalArgumentException("Api token has expired"))
        }
        if (token.user.role != UserRole.ADMIN) {
            return@lockedTransaction Result.failure(IllegalArgumentException("Api token user is not an admin"))
        }
        token.lastUsedAt = now
        Result.success(token.user)
    }
}
