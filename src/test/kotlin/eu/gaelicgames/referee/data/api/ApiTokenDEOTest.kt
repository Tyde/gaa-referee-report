package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.ApiToken
import eu.gaelicgames.referee.data.User
import eu.gaelicgames.referee.data.UserRole
import eu.gaelicgames.referee.util.lockedTransaction
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.time.LocalDateTime

class ApiTokenDEOTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp(): Unit {
            TestHelper.setupDatabase()
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            TestHelper.tearDownDatabase()
        }
    }

    private data class StoredToken(
        val tokenHash: String,
        val revoked: Boolean,
        val lastUsedAt: LocalDateTime?,
        val userId: Long
    )

    private suspend fun createUser(role: UserRole, mail: String): User {
        return lockedTransaction {
            User.newWithPassword("Test", "User", mail, "12345", role)
        }
    }

    private suspend fun storedToken(id: Long): StoredToken? {
        return lockedTransaction {
            ApiToken.findById(id)?.let {
                StoredToken(
                    tokenHash = it.tokenHash,
                    revoked = it.revoked,
                    lastUsedAt = it.lastUsedAt,
                    userId = it.user.id.value
                )
            }
        }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `create returns plaintext token with gge_ prefix and stores only the hash`() {
        runBlocking {
            val admin = createUser(UserRole.ADMIN, "admin-create@test.de")
            val result = NewApiTokenDEO(name = "Admin API Token", expiresInDays = 30).createInDatabase(admin)

            assert(result.isSuccess) { "Token creation failed: ${result.exceptionOrNull()}" }
            val created = result.getOrThrow()

            assert(created.token.startsWith("gge_")) { "Token should be prefixed with gge_, was: ${created.token}" }
            assert(created.token.length == 4 + 43) { "Token should be gge_ + 32 random bytes base64url encoded, was: ${created.token}" }
            assert(created.name == "Admin API Token")
            assert(created.expiresAt != null) { "expiresAt should be set" }
            assert(LocalDateTime.parse(created.expiresAt!!).isAfter(LocalDateTime.now())) {
                "expiresAt should be a parseable ISO-8601 string in the future"
            }

            val stored = storedToken(created.id)
            assert(stored != null) { "Api token should be stored in the database" }
            assert(stored!!.tokenHash == sha256Hex(created.token)) {
                "Database should store the SHA-256 hex of the plaintext token"
            }
            assert(stored.tokenHash != created.token) { "Database must not store the plaintext token" }
            assert(stored.tokenHash.length == 64) { "Token hash should be 64 hex characters" }
            assert(!stored.revoked) { "Token should not be revoked on creation" }
            assert(stored.lastUsedAt == null) { "lastUsedAt should be null on creation" }
            assert(stored.userId == admin.id.value) { "Token should be linked to the given admin user" }
        }
    }

    @Test
    fun `validate succeeds for a fresh token and bumps lastUsedAt`() {
        runBlocking {
            val admin = createUser(UserRole.ADMIN, "admin-validate@test.de")
            val created = NewApiTokenDEO(name = "token", expiresInDays = 30).createInDatabase(admin).getOrThrow()

            val before = storedToken(created.id)
            assert(before?.lastUsedAt == null) { "lastUsedAt should be null before validation" }

            val result = validateApiToken(created.token)

            assert(result.isSuccess) { "Fresh token validation should succeed: ${result.exceptionOrNull()}" }
            assert(result.getOrThrow().id == admin.id) { "Validation should return the linked admin user" }

            val after = storedToken(created.id)
            assert(after?.lastUsedAt != null) { "lastUsedAt should be updated after validation" }
        }
    }

    @Test
    fun `validate rejects unknown token`() {
        runBlocking {
            val result = validateApiToken("gge_unknownTokenThatWasNeverCreated")
            assert(result.isFailure) { "Unknown token should be rejected" }
        }
    }

    @Test
    fun `validate rejects revoked token`() {
        runBlocking {
            val admin = createUser(UserRole.ADMIN, "admin-revoked@test.de")
            val created = NewApiTokenDEO(name = "token", expiresInDays = 30).createInDatabase(admin).getOrThrow()
            lockedTransaction {
                ApiToken.findById(created.id)?.let { it.revoked = true }
            }

            val result = validateApiToken(created.token)
            assert(result.isFailure) { "Revoked token should be rejected" }
        }
    }

    @Test
    fun `validate rejects expired token`() {
        runBlocking {
            val admin = createUser(UserRole.ADMIN, "admin-expired@test.de")
            val created = NewApiTokenDEO(name = "token", expiresInDays = 30).createInDatabase(admin).getOrThrow()
            lockedTransaction {
                ApiToken.findById(created.id)?.let { it.expiresAt = LocalDateTime.now().minusDays(1) }
            }

            val result = validateApiToken(created.token)
            assert(result.isFailure) { "Expired token should be rejected" }
        }
    }

    @Test
    fun `validate rejects token of a non-admin user`() {
        runBlocking {
            val referee = createUser(UserRole.REFEREE, "referee-nonadmin@test.de")
            val created = NewApiTokenDEO(name = "token", expiresInDays = 30).createInDatabase(referee).getOrThrow()

            val result = validateApiToken(created.token)
            assert(result.isFailure) { "Token of a non-admin user should be rejected" }
        }
    }

    @Test
    fun `revoke marks the token as revoked and validation rejects it afterwards`() {
        runBlocking {
            val admin = createUser(UserRole.ADMIN, "admin-revoke@test.de")
            val created = NewApiTokenDEO(name = "token", expiresInDays = 30).createInDatabase(admin).getOrThrow()

            val revokeResult = RevokeApiTokenDEO(id = created.id).revoke()

            assert(revokeResult.isSuccess) { "Revoke should succeed: ${revokeResult.exceptionOrNull()}" }
            assert(revokeResult.getOrThrow().revoked) { "Revoked DEO should report revoked = true" }

            val stored = storedToken(created.id)
            assert(stored?.revoked == true) { "Token should be marked revoked in the database" }

            val validateResult = validateApiToken(created.token)
            assert(validateResult.isFailure) { "Revoked token should be rejected" }
        }
    }

    @Test
    fun `revoke fails for an unknown token id`() {
        runBlocking {
            val result = RevokeApiTokenDEO(id = 999999L).revoke()
            assert(result.isFailure) { "Revoking an unknown token should fail" }
        }
    }

    @Test
    fun `apiTokenList returns all tokens without leaking the hash`() {
        runBlocking {
            val admin = createUser(UserRole.ADMIN, "admin-list@test.de")
            val first = NewApiTokenDEO(name = "token-a", expiresInDays = 30).createInDatabase(admin).getOrThrow()
            NewApiTokenDEO(name = "token-b", expiresInDays = 30).createInDatabase(admin).getOrThrow()

            val tokens = apiTokenList()

            assert(tokens.size >= 2) { "List should contain the created tokens, was: ${tokens.size}" }
            val firstInList = tokens.first { it.id == first.id }
            assert(firstInList.name == "token-a")
            assert(firstInList.createdAt == lockedTransaction { ApiToken.findById(first.id)!!.createdAt }.toString()) {
                "createdAt should be serialized as a string"
            }
            assert(!firstInList.revoked)
        }
    }
}
