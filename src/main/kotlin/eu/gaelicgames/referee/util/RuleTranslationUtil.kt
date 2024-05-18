package eu.gaelicgames.referee.util

import eu.gaelicgames.referee.data.api.RuleTranslation
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object RuleTranslationUtil {

    private val SYSTEM_PROMT = """
        You're a translation assistant for GAA rules. 
        You will be getting messages with rules for Gaelic Football, Ladies Gaelic Football, Camogie or Hurling, which you will reply to with the translation of these rules in french, german, and spanish in the following JSON format:
        {
        "ruleEn": "{Original Rule}",
        "ruleFr": "{Rule translated in French}",
        "ruleDe": "{Rule translated in German}",
        "ruleEs": "{Rule translated in Spanish}"
        }
        Try to make the translation precise as we are talking about rules. Nevertheless try to make the language as fitting to the local language as possible. Also try to ensure that the intent of the rule stays the same. Note: Sometimes at the beginning of the rule there is the following action of the rule: e.g. CAUTION for giving a yellow card or ORDER OFF for giving a red card
        
        Also try to output valid json that can be parsed by the system. This means that you should escape special characters like " with \"
    """.trimIndent()

    private val CLAUDE_ENDPOINT = "https://api.anthropic.com/v1/messages"
    private val CLAUDE_MODEL = "claude-3-sonnet-20240229"

    private val ktorClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }


    @Serializable
    enum class Role {
        @SerialName("user")
        USER,

        @SerialName("assistant")
        ASSISTANT
    }

    @Serializable
    data class ClaudeMessage(
        val role: Role,
        val content: String
    )

    @Serializable
    data class ClaudeMessageRequest(
        val model: String,
        val messages: List<ClaudeMessage>,
        @SerialName("max_tokens")
        val maxTokens: Int,
        val temperature: Double? = null,
        val system: String? = null
    )

    @Serializable
    data class ClaudeMessageResponseContent(
        val text: String,
        val type: String
    )

    @Serializable
    data class ClaudeMessageResponse(
        val content: List<ClaudeMessageResponseContent>,
        val id: String,
        val model: String,
        val role: Role,
        @SerialName("stop_reason")
        val stopReason: String,
        @SerialName("stop_sequence")
        val stopSequence: String?,
        val type: String,
    )


    suspend fun translateRule(rule: String): Result<RuleTranslation> {
        val requestPayload = ClaudeMessageRequest(
            model = CLAUDE_MODEL,
            messages = listOf(ClaudeMessage(Role.USER, rule)),
            maxTokens = 500,
            system = SYSTEM_PROMT
        )

        val claudeAccessToken = GGERefereeConfig.claudeAccessToken
        val response = ktorClient.post(CLAUDE_ENDPOINT) {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
                append("x-api-key", claudeAccessToken)
                append("anthropic-version", "2023-06-01")
            }
            setBody(requestPayload)
        }

        val responseContent: ClaudeMessageResponse = response.body()

        return kotlin.runCatching {
            Json.decodeFromString<RuleTranslation>(responseContent.content.first().text)
        }

    }


}
