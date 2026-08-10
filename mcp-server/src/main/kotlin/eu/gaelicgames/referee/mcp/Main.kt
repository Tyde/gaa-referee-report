package eu.gaelicgames.referee.mcp

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import io.modelcontextprotocol.kotlin.sdk.types.error
import io.modelcontextprotocol.kotlin.sdk.types.success
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

data class MCPConfig(val serverUrl: String, val apiToken: String)

class ApiCallException(message: String) : Exception(message)

@Serializable
data class TeamDEO(
    val name: String,
    val id: Long,
    val isAmalgamation: Boolean,
    val amalgamationTeams: List<TeamDEO>? = null,
)

@Serializable
data class MergeTeamsDEO(val baseTeam: Long, val teamsToMerge: List<Long>)

@Serializable
data class NewAmalgamationDEO(val name: String, val teams: List<TeamDEO>)

@Serializable
data class MergeTournamentDEO(val mergeFromId: Long, val mergeToId: Long)

@Serializable
data class ApiError(val error: String, val message: String)

private val json = Json { ignoreUnknownKeys = true }

private val prettyJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

fun loadConfigFromEnv(): MCPConfig {
    val serverUrl = System.getenv("GGE_SERVER_URL")?.trim()?.trimEnd('/')
    val apiToken = System.getenv("GGE_API_TOKEN")?.trim()
    if (serverUrl.isNullOrEmpty() || apiToken.isNullOrEmpty()) {
        System.err.println(
            "Error: GGE_SERVER_URL and GGE_API_TOKEN environment variables are required.\n" +
                "  GGE_SERVER_URL: base URL of the GAA Referee Report server (e.g. https://referee.example.com)\n" +
                "  GGE_API_TOKEN:  an admin API token (gge_...) for that server"
        )
        kotlin.system.exitProcess(1)
    }
    return MCPConfig(serverUrl, apiToken)
}

fun createHttpClient(config: MCPConfig, engine: HttpClientEngine = CIO.create()): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) {
        json(json)
    }
    install(DefaultRequest) {
        url(config.serverUrl + "/")
        header(HttpHeaders.Authorization, "Bearer ${config.apiToken}")
    }
}

private suspend fun HttpResponse.prettyResultOrThrow(): String {
    if (status.value in 200..299) {
        val body = bodyAsText()
        if (body.isBlank()) return "{}"
        return prettyJson.encodeToString(
            JsonElement.serializer(),
            prettyJson.parseToJsonElement(body)
        )
    }
    val message = runCatching {
        json.decodeFromString<ApiError>(bodyAsText()).message
    }.getOrElse { "HTTP ${status.value} ${status.description}" }
    throw ApiCallException(message)
}

suspend fun listTeams(client: HttpClient): String =
    client.get("api/teams_available").prettyResultOrThrow()

suspend fun updateTeam(client: HttpClient, team: TeamDEO): String =
    client.post("api/team/update") {
        contentType(ContentType.Application.Json)
        setBody(team)
    }.prettyResultOrThrow()

suspend fun mergeTeams(client: HttpClient, mergeTeamsDEO: MergeTeamsDEO): String =
    client.post("api/team/merge") {
        contentType(ContentType.Application.Json)
        setBody(mergeTeamsDEO)
    }.prettyResultOrThrow()

suspend fun createAmalgamation(client: HttpClient, newAmalgamationDEO: NewAmalgamationDEO): String =
    client.post("api/new_amalgamation") {
        contentType(ContentType.Application.Json)
        setBody(newAmalgamationDEO)
    }.prettyResultOrThrow()

suspend fun mergeTournaments(client: HttpClient, mergeTournamentDEO: MergeTournamentDEO): String =
    client.post("api/tournament/merge") {
        contentType(ContentType.Application.Json)
        setBody(mergeTournamentDEO)
    }.prettyResultOrThrow()

private suspend fun <T> callTool(
    arguments: JsonObject?,
    decode: (JsonObject?) -> T,
    invoke: suspend (T) -> String,
): CallToolResult = try {
    CallToolResult.success(invoke(decode(arguments)))
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    CallToolResult.error(e.message ?: "${e::class.simpleName ?: "Unknown"} error")
}

private fun intSchema(description: String) = buildJsonObject {
    put("type", "integer")
    put("description", description)
}

private fun stringSchema(description: String) = buildJsonObject {
    put("type", "string")
    put("description", description)
}

private fun booleanSchema(description: String) = buildJsonObject {
    put("type", "boolean")
    put("description", description)
}

private fun arraySchema(description: String, itemsType: String = "integer") = buildJsonObject {
    put("type", "array")
    put("description", description)
    put("items", buildJsonObject { put("type", itemsType) })
}

fun buildMcpServer(client: HttpClient): Server = Server(
    Implementation(
        name = "gaa-referee-admin",
        version = "1.0.0",
    ),
    ServerOptions(
        capabilities = ServerCapabilities(
            tools = ServerCapabilities.Tools(listChanged = true),
        ),
    ),
    instructions = "Admin tools to manage teams, amalgamations and tournaments on the GAA Referee Report server.",
) {
    addTool(
        name = "list_teams",
        description = "List all teams (including amalgamations) available on the server.",
        inputSchema = ToolSchema(properties = buildJsonObject {}),
    ) { request ->
        callTool<Unit>(request.params.arguments, { Unit }, { listTeams(client) })
    }

    addTool(
        name = "update_team",
        description = "Update a team's name and amalgamation membership. " +
            "isAmalgamation must be true and amalgamationTeams provided to edit amalgamation membership.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("id", intSchema("Team id to update"))
                put("name", stringSchema("New team name"))
                put("isAmalgamation", booleanSchema("Whether the team is an amalgamation"))
                put(
                    "amalgamationTeams",
                    arraySchema("Optional list of member team ids for an amalgamation", itemsType = "object")
                )
            },
            required = listOf("id", "name", "isAmalgamation"),
        ),
    ) { request ->
        callTool(request.params.arguments, { decodeTeamArguments(it) }) { updateTeam(client, it) }
    }

    addTool(
        name = "merge_teams",
        description = "Merge several teams into a base team.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("baseTeam", intSchema("Id of the team to merge into"))
                put("teamsToMerge", arraySchema("Ids of the teams to merge into the base team"))
            },
            required = listOf("baseTeam", "teamsToMerge"),
        ),
    ) { request ->
        callTool(request.params.arguments, { decodeMergeTeamsArguments(it) }) { mergeTeams(client, it) }
    }

    addTool(
        name = "create_amalgamation",
        description = "Create a new amalgamation from existing teams.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("name", stringSchema("Name of the new amalgamation"))
                put("teams", arraySchema("Teams that form the amalgamation", itemsType = "object"))
            },
            required = listOf("name", "teams"),
        ),
    ) { request ->
        callTool(request.params.arguments, { decodeNewAmalgamationArguments(it) }) { createAmalgamation(client, it) }
    }

    addTool(
        name = "merge_tournaments",
        description = "Merge one tournament into another.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("mergeFromId", intSchema("Id of the tournament to merge away"))
                put("mergeToId", intSchema("Id of the tournament to merge into"))
            },
            required = listOf("mergeFromId", "mergeToId"),
        ),
    ) { request ->
        callTool(request.params.arguments, { decodeMergeTournamentArguments(it) }) { mergeTournaments(client, it) }
    }
}

private fun decodeTeamArguments(arguments: JsonObject?): TeamDEO {
    val args = arguments ?: throw ApiCallException("Missing tool arguments")
    val id = args["id"]?.jsonPrimitive?.longOrNull ?: throw ApiCallException("Missing or invalid 'id'")
    val name = args["name"]?.jsonPrimitive?.contentOrNull ?: throw ApiCallException("Missing or invalid 'name'")
    val isAmalgamation = args["isAmalgamation"]?.jsonPrimitive?.booleanOrNull
        ?: throw ApiCallException("Missing or invalid 'isAmalgamation'")
    val amalgamationTeams = args["amalgamationTeams"]?.jsonArray?.map { element ->
        json.decodeFromJsonElement(TeamDEO.serializer(), element)
    }
    return TeamDEO(name = name, id = id, isAmalgamation = isAmalgamation, amalgamationTeams = amalgamationTeams)
}

private fun decodeMergeTeamsArguments(arguments: JsonObject?): MergeTeamsDEO {
    val args = arguments ?: throw ApiCallException("Missing tool arguments")
    val baseTeam = args["baseTeam"]?.jsonPrimitive?.longOrNull ?: throw ApiCallException("Missing or invalid 'baseTeam'")
    val teamsToMerge = args["teamsToMerge"]?.jsonArray?.map { it.jsonPrimitive.long } ?: emptyList()
    return MergeTeamsDEO(baseTeam = baseTeam, teamsToMerge = teamsToMerge)
}

private fun decodeNewAmalgamationArguments(arguments: JsonObject?): NewAmalgamationDEO {
    val args = arguments ?: throw ApiCallException("Missing tool arguments")
    val name = args["name"]?.jsonPrimitive?.contentOrNull ?: throw ApiCallException("Missing or invalid 'name'")
    val teams = args["teams"]?.jsonArray?.map { element ->
        json.decodeFromJsonElement(TeamDEO.serializer(), element)
    } ?: emptyList()
    return NewAmalgamationDEO(name = name, teams = teams)
}

private fun decodeMergeTournamentArguments(arguments: JsonObject?): MergeTournamentDEO {
    val args = arguments ?: throw ApiCallException("Missing tool arguments")
    val mergeFromId = args["mergeFromId"]?.jsonPrimitive?.longOrNull
        ?: throw ApiCallException("Missing or invalid 'mergeFromId'")
    val mergeToId = args["mergeToId"]?.jsonPrimitive?.longOrNull
        ?: throw ApiCallException("Missing or invalid 'mergeToId'")
    return MergeTournamentDEO(mergeFromId = mergeFromId, mergeToId = mergeToId)
}

fun main() {
    System.setProperty("kotlin-logging.logStartupMessage", "false")
    val config = loadConfigFromEnv()
    val client = createHttpClient(config)
    val server = buildMcpServer(client)

    runBlocking {
        val transport = StdioServerTransport(
            input = System.`in`.asSource().buffered(),
            output = System.out.asSink().buffered(),
        )
        val done = Job()
        server.onClose { done.complete() }
        server.createSession(transport)
        done.join()
    }
}
