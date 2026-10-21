package eu.gaelicgames.referee.util

import java.text.Normalizer

/**
 * Characters that carry no combining mark and therefore survive NFD
 * decomposition, so they need an explicit expansion.
 * Mirrors the map in frontend-vite/src/utils/team_name_normalizer.ts.
 */
private val EXTRA_EXPANSIONS = mapOf(
    'ß' to "ss",
    'æ' to "ae",
    'œ' to "oe",
    'ø' to "o",
    'đ' to "d",
    'ð' to "d",
    'þ' to "th",
    'ł' to "l"
)

private val COMBINING_MARKS = Regex("\\p{Mn}+")
private val WHITESPACE_RUN = Regex("\\s+")

/**
 * Search/uniqueness key for a team name: lowercased, diacritics stripped,
 * whitespace collapsed. Punctuation is deliberately preserved, so
 * "Saint-Brieuc" and "Saint Brieuc" stay distinct.
 *
 * This is the only place team names may be normalized on the backend.
 * Changing these rules requires re-normalizing every TeamAliases row.
 */
fun normalizeTeamName(input: String): String {
    val expanded = buildString {
        for (char in input.lowercase()) {
            append(EXTRA_EXPANSIONS[char] ?: char)
        }
    }
    return Normalizer.normalize(expanded, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .replace(WHITESPACE_RUN, " ")
        .trim()
}
