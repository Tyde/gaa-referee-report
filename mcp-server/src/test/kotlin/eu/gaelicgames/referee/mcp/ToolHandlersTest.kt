package eu.gaelicgames.referee.mcp

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val testConfig = MCPConfig(serverUrl = "https://referee.example.com", apiToken = "gge_testtoken")

private fun mockClient(
    handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData,
): io.ktor.client.HttpClient = createHttpClient(testConfig, MockEngine { handler(it) })

private fun MockRequestHandleScope.jsonResponse(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): io.ktor.client.request.HttpResponseData {
    val headers = headersOf(HttpHeaders.ContentType, "application/json")
    return respond(content = body, status = status, headers = headers)
}

private val json = Json

class ToolHandlersTest {

    @Test
    fun `list_teams hits GET teams_available and returns pretty JSON`() = runBlocking {
        val teams = """[{"name":"Team A","id":1,"isAmalgamation":false,"amalgamationTeams":null}]"""
        val client = mockClient { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/api/teams_available", request.url.encodedPath)
            assertEquals("Bearer gge_testtoken", request.headers[HttpHeaders.Authorization])
            jsonResponse(teams)
        }
        val result = listTeams(client)
        assertTrue(result.contains("Team A"))
        assertTrue(Json.parseToJsonElement(result) is kotlinx.serialization.json.JsonArray)
    }

    @Test
    fun `list_teams maps ApiError to thrown exception`() = runBlocking {
        val client = mockClient {
            jsonResponse("""{"error":"notAuthorized","message":"API token is not valid"}""", HttpStatusCode.Unauthorized)
        }
        val thrown = runCatching { listTeams(client) }.exceptionOrNull()
        assertTrue(thrown is ApiCallException)
        assertEquals("API token is not valid", thrown.message)
    }

    @Test
    fun `update_team posts TeamDEO to team update`() = runBlocking {
        val client = mockClient { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/team/update", request.url.encodedPath)
            assertEquals("Bearer gge_testtoken", request.headers[HttpHeaders.Authorization])
            val body = (request.body as io.ktor.http.content.TextContent).text
            val sent = json.decodeFromString(TeamDEO.serializer(), body)
            assertEquals(1L, sent.id)
            assertEquals("New Name", sent.name)
            jsonResponse("""{"name":"New Name","id":1,"isAmalgamation":false,"amalgamationTeams":null}""")
        }
        val result = updateTeam(client, TeamDEO(name = "New Name", id = 1, isAmalgamation = false))
        assertTrue(result.contains("New Name"))
    }

    @Test
    fun `update_team maps ApiError to thrown exception`() = runBlocking {
        val client = mockClient {
            jsonResponse("""{"error":"insertionFailed","message":"Team not found"}""", HttpStatusCode.BadRequest)
        }
        val thrown = runCatching { updateTeam(client, TeamDEO(name = "New Name", id = 999, isAmalgamation = false)) }
            .exceptionOrNull()
        assertTrue(thrown is ApiCallException)
        assertEquals("Team not found", thrown.message)
    }

    @Test
    fun `merge_teams posts MergeTeamsDEO to team merge`() = runBlocking {
        val client = mockClient { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/team/merge", request.url.encodedPath)
            val body = (request.body as io.ktor.http.content.TextContent).text
            val sent = json.decodeFromString(MergeTeamsDEO.serializer(), body)
            assertEquals(1L, sent.baseTeam)
            assertEquals(listOf(2L, 3L), sent.teamsToMerge)
            jsonResponse("""{"name":"Merged","id":1,"isAmalgamation":false,"amalgamationTeams":null}""")
        }
        val result = mergeTeams(client, MergeTeamsDEO(baseTeam = 1, teamsToMerge = listOf(2, 3)))
        assertTrue(result.contains("Merged"))
    }

    @Test
    fun `merge_teams maps ApiError to thrown exception`() = runBlocking {
        val client = mockClient {
            jsonResponse("""{"error":"illegalArgument","message":"Cannot merge team into itself"}""", HttpStatusCode.BadRequest)
        }
        val thrown = runCatching { mergeTeams(client, MergeTeamsDEO(baseTeam = 1, teamsToMerge = listOf(1))) }
            .exceptionOrNull()
        assertTrue(thrown is ApiCallException)
        assertEquals("Cannot merge team into itself", thrown.message)
    }

    @Test
    fun `create_amalgamation posts NewAmalgamationDEO to new amalgamation`() = runBlocking {
        val client = mockClient { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/new_amalgamation", request.url.encodedPath)
            val body = (request.body as io.ktor.http.content.TextContent).text
            val sent = json.decodeFromString(NewAmalgamationDEO.serializer(), body)
            assertEquals("County XI", sent.name)
            assertEquals(1, sent.teams.size)
            jsonResponse("""{"name":"County XI","id":5,"isAmalgamation":true,"amalgamationTeams":null}""")
        }
        val result = createAmalgamation(
            client,
            NewAmalgamationDEO(
                name = "County XI",
                teams = listOf(TeamDEO(name = "Team A", id = 1, isAmalgamation = false)),
            ),
        )
        assertTrue(result.contains("County XI"))
    }

    @Test
    fun `create_amalgamation maps ApiError to thrown exception`() = runBlocking {
        val client = mockClient {
            jsonResponse("""{"error":"insertionFailed","message":"Teams are already part of another amalgamation"}""", HttpStatusCode.BadRequest)
        }
        val thrown = runCatching {
            createAmalgamation(
                client,
                NewAmalgamationDEO(name = "County XI", teams = listOf(TeamDEO(name = "Team A", id = 1, isAmalgamation = false))),
            )
        }.exceptionOrNull()
        assertTrue(thrown is ApiCallException)
        assertEquals("Teams are already part of another amalgamation", thrown.message)
    }

    @Test
    fun `merge_tournaments posts MergeTournamentDEO to tournament merge`() = runBlocking {
        val client = mockClient { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/tournament/merge", request.url.encodedPath)
            val body = (request.body as io.ktor.http.content.TextContent).text
            val sent = json.decodeFromString(MergeTournamentDEO.serializer(), body)
            assertEquals(10L, sent.mergeFromId)
            assertEquals(20L, sent.mergeToId)
            jsonResponse("""{"id":20,"name":"Surviving","location":"Dublin","date":"2026-08-10","region":1}""")
        }
        val result = mergeTournaments(client, MergeTournamentDEO(mergeFromId = 10, mergeToId = 20))
        assertTrue(result.contains("Surviving"))
    }

    @Test
    fun `merge_tournaments maps ApiError to thrown exception`() = runBlocking {
        val client = mockClient {
            jsonResponse("""{"error":"notFound","message":"Tournament not found"}""", HttpStatusCode.NotFound)
        }
        val thrown = runCatching { mergeTournaments(client, MergeTournamentDEO(mergeFromId = 10, mergeToId = 999)) }
            .exceptionOrNull()
        assertTrue(thrown is ApiCallException)
        assertEquals("Tournament not found", thrown.message)
    }

    @Test
    fun `non ApiError failure surfaces HTTP status message`() = runBlocking {
        val client = mockClient {
            respond(content = "bad gateway", status = HttpStatusCode.BadGateway, headers = headersOf(HttpHeaders.ContentType, "text/plain"))
        }
        val thrown = runCatching { listTeams(client) }.exceptionOrNull()
        assertTrue(thrown is ApiCallException)
        assertTrue(thrown.message.orEmpty().contains("502"))
    }
}
