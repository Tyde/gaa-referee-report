package eu.gaelicgames.referee.plugins

import eu.gaelicgames.referee.data.User
import eu.gaelicgames.referee.data.UserRole
import eu.gaelicgames.referee.data.api.NewApiTokenDEO
import eu.gaelicgames.referee.data.api.RevokeApiTokenDEO
import eu.gaelicgames.referee.data.api.TestHelper
import eu.gaelicgames.referee.data.api.createInDatabase
import eu.gaelicgames.referee.data.api.revoke
import eu.gaelicgames.referee.util.lockedTransaction
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ApiTokenAuthTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp() {
            TestHelper.setupDatabase()
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            TestHelper.tearDownDatabase()
        }
    }

    private fun Application.testModule() {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        install(Authentication) {
            configureApiTokenAuth()
        }
        routing {
            authenticate("auth-api-token") {
                get("/protected") {
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }

    private suspend fun createAdminUser(mail: String): User {
        return lockedTransaction {
            User.newWithPassword("Test", "Admin", mail, "12345", UserRole.ADMIN)
        }
    }

    @Test
    fun `request without bearer token is rejected with 401`() = testApplication {
        application { testModule() }
        val response = client.get("/protected")
        assert(response.status == HttpStatusCode.Unauthorized)
    }

    @Test
    fun `garbage token is rejected with 401`() = testApplication {
        application { testModule() }
        val response = client.get("/protected") {
            header(HttpHeaders.Authorization, "Bearer garbage_token_that_does_not_exist")
        }
        assert(response.status == HttpStatusCode.Unauthorized)
    }

    @Test
    fun `valid token authenticates and returns 200`() = runBlocking {
        val admin = createAdminUser("token-auth@test.de")
        val created = NewApiTokenDEO(name = "http test", expiresInDays = 30).createInDatabase(admin).getOrThrow()
        testApplication {
            application { testModule() }
            val response = client.get("/protected") {
                header(HttpHeaders.Authorization, "Bearer ${created.token}")
            }
            assert(response.status == HttpStatusCode.OK)
        }
    }

    @Test
    fun `revoked token is rejected with 401`() = runBlocking {
        val admin = createAdminUser("token-revoked@test.de")
        val created = NewApiTokenDEO(name = "http test", expiresInDays = 30).createInDatabase(admin).getOrThrow()
        RevokeApiTokenDEO(id = created.id).revoke()
        testApplication {
            application { testModule() }
            val response = client.get("/protected") {
                header(HttpHeaders.Authorization, "Bearer ${created.token}")
            }
            assert(response.status == HttpStatusCode.Unauthorized)
        }
    }
}
