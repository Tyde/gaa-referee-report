package eu.gaelicgames.referee.mcp

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

@Serializable
private data class BackendFaithfulTeamDEO(
    val name: String,
    val id: Long,
    val isAmalgamation: Boolean,
    val amalgamationTeams: List<BackendFaithfulTeamDEO>?,
)

@Serializable
private data class BackendFaithfulNewAmalgamationDEO(
    val name: String,
    val teams: List<BackendFaithfulTeamDEO>,
)

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
        val result = updateTeam(client, TeamDEO(name = "New Name", id = 1, isAmalgamation = false, amalgamationTeams = null))
        assertTrue(result.contains("New Name"))
    }

    @Test
    fun `update_team body matches backend wire contract with required amalgamationTeams`() = runBlocking {
        val client = mockClient { request ->
            val body = (request.body as io.ktor.http.content.TextContent).text
            assertTrue(body.contains("amalgamationTeams"), "body must always carry amalgamationTeams: $body")
            val sent = json.decodeFromString(BackendFaithfulTeamDEO.serializer(), body)
            assertEquals("New Name", sent.name)
            assertEquals(1L, sent.id)
            assertEquals(null, sent.amalgamationTeams)
            jsonResponse("""{"name":"New Name","id":1,"isAmalgamation":false,"amalgamationTeams":null}""")
        }
        updateTeam(client, TeamDEO(name = "New Name", id = 1, isAmalgamation = false, amalgamationTeams = null))
    }

    @Test
    fun `update_team maps ApiError to thrown exception`() = runBlocking {
        val client = mockClient {
            jsonResponse("""{"error":"insertionFailed","message":"Team not found"}""", HttpStatusCode.BadRequest)
        }
        val thrown = runCatching {
            updateTeam(client, TeamDEO(name = "New Name", id = 999, isAmalgamation = false, amalgamationTeams = null))
        }
            .exceptionOrNull()
        assertTrue(thrown is ApiCallException)
        assertEquals("Team not found", thrown.message)
    }

    @Test
    fun `update_team 200 with ApiError body surfaces as tool error with message`() = runBlocking {
        val client = mockClient {
            jsonResponse("""{"error":"insertionFailed","message":"Team not found"}""", HttpStatusCode.OK)
        }
        val thrown = runCatching {
            updateTeam(client, TeamDEO(name = "New Name", id = 999, isAmalgamation = false, amalgamationTeams = null))
        }
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
                teams = listOf(TeamDEO(name = "Team A", id = 1, isAmalgamation = false, amalgamationTeams = null)),
            ),
        )
        assertTrue(result.contains("County XI"))
    }

    @Test
    fun `create_amalgamation body matches backend wire contract with required amalgamationTeams`() = runBlocking {
        val client = mockClient { request ->
            val body = (request.body as io.ktor.http.content.TextContent).text
            assertTrue(body.contains("amalgamationTeams"), "body must always carry amalgamationTeams: $body")
            val sent = json.decodeFromString(BackendFaithfulNewAmalgamationDEO.serializer(), body)
            assertEquals("County XI", sent.name)
            assertEquals(1L, sent.teams.single().id)
            assertEquals(null, sent.teams.single().amalgamationTeams)
            jsonResponse("""{"name":"County XI","id":5,"isAmalgamation":true,"amalgamationTeams":null}""")
        }
        createAmalgamation(
            client,
            NewAmalgamationDEO(
                name = "County XI",
                teams = listOf(TeamDEO(name = "Team A", id = 1, isAmalgamation = false, amalgamationTeams = null)),
            ),
        )
    }

    @Test
    fun `create_amalgamation maps ApiError to thrown exception`() = runBlocking {
        val client = mockClient {
            jsonResponse("""{"error":"insertionFailed","message":"Teams are already part of another amalgamation"}""", HttpStatusCode.BadRequest)
        }
        val thrown = runCatching {
            createAmalgamation(
                client,
                NewAmalgamationDEO(name = "County XI", teams = listOf(TeamDEO(name = "Team A", id = 1, isAmalgamation = false, amalgamationTeams = null))),
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

    @Test
    fun `merge_teams missing required teamsToMerge yields tool error naming the argument`() = runBlocking {
        val client = mockClient { jsonResponse("{}") }
        val result = callTool(
            buildJsonObject { put("baseTeam", 1) },
            { decodeMergeTeamsArguments(it) },
            { mergeTeams(client, it) },
        )
        assertTrue(result.isError == true)
        val text = (result.content.first() as TextContent).text
        assertTrue(text.contains("teamsToMerge"))
    }

    @Test
    fun `create_amalgamation missing required teams yields tool error naming the argument`() = runBlocking {
        val client = mockClient { jsonResponse("{}") }
        val result = callTool(
            buildJsonObject { put("name", "County XI") },
            { decodeNewAmalgamationArguments(it) },
            { createAmalgamation(client, it) },
        )
        assertTrue(result.isError == true)
        val text = (result.content.first() as TextContent).text
        assertTrue(text.contains("teams"))
    }

    @Test
    fun `update_team with explicit null amalgamationTeams decodes to null`() {
        val team = decodeTeamArguments(
            buildJsonObject {
                put("id", 1)
                put("name", "New Name")
                put("isAmalgamation", false)
                put("amalgamationTeams", null)
            }
        )
        assertEquals(null, team.amalgamationTeams)
    }

    @Test
    fun `object array schemas describe nested TeamDEO properties`() = runBlocking {
        val server = buildMcpServer(mockClient { jsonResponse("[]") })
        val updateSchema = server.tools.getValue("update_team").tool.inputSchema
        val updateItems = updateSchema.properties!!
            .getValue("amalgamationTeams").jsonObject
            .getValue("items").jsonObject
        val updateProps = updateItems.getValue("properties").jsonObject
        assertTrue(updateProps.containsKey("id"))
        assertTrue(updateProps.containsKey("name"))
        assertTrue(updateProps.containsKey("isAmalgamation"))
        assertFalse(updateProps.containsKey("amalgamationTeams"))
        assertTrue(updateSchema.properties!!
            .getValue("amalgamationTeams").jsonObject
            .getValue("description").toString().contains("id"))

        val createSchema = server.tools.getValue("create_amalgamation").tool.inputSchema
        val createProps = createSchema.properties!!
            .getValue("teams").jsonObject
            .getValue("items").jsonObject
            .getValue("properties").jsonObject
        assertTrue(createProps.containsKey("id"))
        assertTrue(createProps.containsKey("name"))
        assertTrue(createProps.containsKey("isAmalgamation"))
        assertFalse(createProps.containsKey("amalgamationTeams"))
    }
}
