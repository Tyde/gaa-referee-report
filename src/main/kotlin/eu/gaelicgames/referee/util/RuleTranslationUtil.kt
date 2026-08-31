package eu.gaelicgames.referee.util

import eu.gaelicgames.referee.data.api.RuleTranslation
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

object RuleTranslationUtil {

    private val logger = LoggerFactory.getLogger(RuleTranslationUtil::class.java)

    private val SYSTEM_PROMT = """You are a translation assistant for official GAA playing rules. Each input is a single
rule from one of four codes: Gaelic Football, Ladies Gaelic Football, Camogie, or Hurling.
Note that terminology differs by code (e.g. Hurling/Camogie use a sliotar struck with a
hurley/camán; Football is kicked and hand-passed) — choose nouns that match the code.

OUTPUT CONTRACT
Reply with ONLY a single raw JSON object, no surrounding text, no markdown code fences,
no comments. It must be valid JSON with all quotes and special characters properly escaped.
Use exactly these four keys in this order:
{
  "ruleEn": "<the original rule, verbatim>",
  "ruleFr": "<French translation>",
  "ruleDe": "<German translation>",
  "ruleEs": "<Spanish translation>"
}

TRANSLATION QUALITY
- Write each translation the way it would appear in that country's official sporting
  rulebook — formal regulatory register, not casual or conversational.
- Do NOT calque English word order or sentence structure. Restructure so the sentence
  reads as if originally written in the target language.
- Render obligation/prohibition with the natural deontic form of each language:
  FR "Il est interdit de…" / "Un joueur ne doit pas…", DE "Ein Spieler darf nicht…",
  ES "Un jugador no debe / no puede…".
- Preserve meaning exactly. Do not add, omit, soften, or reinterpret any rule content;
  regulatory precision matters.
- Keep metric units and numbers; format decimals and lists per locale conventions.
- Be internally consistent: translate a given GAA term the same way every time.

TERMINOLOGY
Translate general sporting terms using the established equivalent in each language.
Keep GAA-specific proper nouns and untranslatable jargon as-is (inline, no parenthetical
glosses or translator's notes). Reference guide:

  Term            FR                          DE                  ES
  free            coup franc                  Freistoß            golpe franco
  penalty         coup de pied de réparation  Strafstoß           penalti
  goal            but                         Tor                 gol
  point           point                       Punkt               punto
  referee         arbitre                     Schiedsrichter      árbitro
  linesman        juge de touche              Linienrichter       juez de línea
  sideline        ligne de touche             Seitenlinie         línea de banda
  hand pass       passe à la main             Handpass            pase con la mano
  small rectangle petite surface              Torraum             área pequeña
  large rectangle grande surface              Strafraum           área grande
  substitute      remplaçant                  Auswechselspieler   suplente
  sin bin         exclusion temporaire        Zeitstrafe          expulsión temporal

  Keep as-is (GAA-specific): sliotar, hurley, camán, puck-out, kick-out, solo (solo run),
  the mark, square ball, "45", "65". For these, build the surrounding sentence idiomatically
  around the kept term.

CARD / DISCIPLINARY ACTIONS
If the rule begins with a disciplinary label such as CAUTION, ORDER OFF, or PENALTY, keep
that label verbatim in English, uppercase, at the very start of every translated field, then
translate the remainder of the rule idiomatically. This keeps the label stable for parsing.

FALLBACK
Never leave a field blank. If a single term is genuinely untranslatable, keep that term in
English inline and translate the rest. Only fall back to the full English text for an entire
field if the rule as a whole cannot be translated at all."""

    private val OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val ktorClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 90_000
        }
    }

    @Serializable
    enum class Role {
        @SerialName("user")
        USER,

        @SerialName("system")
        SYSTEM,

        @SerialName("assistant")
        ASSISTANT
    }

    @Serializable
    data class ChatMessage(
        val role: Role,
        val content: String
    )

    @Serializable
    data class Reasoning(
        val enabled: Boolean? = null,
        val effort: String? = null,
        val exclude: Boolean? = null,
        @SerialName("max_tokens")
        val maxTokens: Int? = null
    )

    @Serializable
    data class ChatCompletionRequest(
        val model: String,
        val messages: List<ChatMessage>,
        @SerialName("max_tokens")
        val maxTokens: Int,
        val temperature: Double? = null,
        val reasoning: Reasoning? = null
    )

    @Serializable
    data class ChatChoiceMessage(
        val role: Role,
        val content: String?,
        val reasoning: String? = null
    )

    @Serializable
    data class ChatChoice(
        val index: Int,
        val message: ChatChoiceMessage,
        @SerialName("finish_reason")
        val finishReason: String?
    )

    @Serializable
    data class ChatCompletionResponse(
        val id: String,
        val model: String,
        val choices: List<ChatChoice>
    )

    @Serializable
    data class OpenRouterErrorDetail(
        val message: String,
        val code: Int? = null
    )

    @Serializable
    data class OpenRouterErrorResponse(
        val error: OpenRouterErrorDetail
    )


    suspend fun translateRule(rule: String): Result<RuleTranslation> {
        val messages = listOf(
            ChatMessage(Role.SYSTEM, SYSTEM_PROMT),
            ChatMessage(Role.USER, rule)
        )

        val requestPayload = ChatCompletionRequest(
            model = GGERefereeConfig.openRouterModel,
            messages = messages,
            maxTokens = 4000,
            reasoning = Reasoning(effort = "minimal")
        )

        val openRouterApiKey = GGERefereeConfig.openRouterApiKey
        if (openRouterApiKey.isBlank()) {
            return Result.failure(IllegalStateException("OPENROUTER_API_KEY is not configured"))
        }

        return kotlin.runCatching {
            val response = ktorClient.post(OPENROUTER_ENDPOINT) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, "Bearer $openRouterApiKey")
                }
                setBody(requestPayload)
            }

            val rawBody = response.bodyAsText()
            logger.debug("OpenRouter API response status: ${response.status}, body: $rawBody")

            if (!response.status.isSuccess()) {
                try {
                    val errorResponse = json.decodeFromString<OpenRouterErrorResponse>(rawBody)
                    throw IllegalStateException("OpenRouter API error (${errorResponse.error.code}): ${errorResponse.error.message}")
                } catch (e: kotlinx.serialization.SerializationException) {
                    throw IllegalStateException("OpenRouter API returned HTTP ${response.status.value}: $rawBody")
                }
            }

            val responseContent: ChatCompletionResponse = response.body()
            val message = responseContent.choices.first().message
            val translationJson = message.content ?: message.reasoning
                ?: throw IllegalStateException("Model returned null content and null reasoning")
            json.decodeFromString<RuleTranslation>(translationJson)
        }
    }


}
