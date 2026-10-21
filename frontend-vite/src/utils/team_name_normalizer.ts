/**
 * Characters that survive NFD decomposition without becoming combining marks,
 * so they need an explicit expansion. This mirrors the backend normalizer.
 */
const extraMap: Record<string, string> = {
    "ß": "ss",
    "æ": "ae",
    "œ": "oe",
    "ø": "o",
    "đ": "d",
    "ð": "d",
    "þ": "th",
    "ł": "l"
};

/**
 * Search key for a team name: lowercased, diacritics stripped, whitespace
 * collapsed. Punctuation is deliberately preserved.
 *
 * This must stay behaviourally identical to normalizeTeamName() on the
 * backend, which decides which aliases are allowed to exist.
 */
export function normalizeForSearch(str: string): string {
    return str
        .toLowerCase()
        .replace(/[ßæœøđðþł]/g, char => extraMap[char] || char)
        .normalize("NFD")
        .replace(/\p{Mn}+/gu, "")
        // Match java.util.regex.Pattern's default ASCII \s class used by
        // the backend, rather than JavaScript's broader Unicode \s class.
        .replace(/[ \t\n\x0B\f\r]+/g, " ")
        .trim();
}
