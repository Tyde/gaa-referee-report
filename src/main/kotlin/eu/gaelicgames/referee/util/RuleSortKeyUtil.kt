package eu.gaelicgames.referee.util

/**
 * Utilities for deriving a stable, sortable key from a rule number.
 */
object RuleSortKeyUtil {

    /**
     * Derives a sortable key from a rule number such as "5.2", "5.10" or "5.34a".
     *
     * The input is tokenized on '.', '_', '-', '/' and space. Each token is split
     * into a leading numeric run and a trailing alphabetic suffix (e.g. "34a" ->
     * numeric "34", suffix "a"; "5" -> "5", ""). The numeric part is zero-padded to
     * 6 digits so lexicographic ordering matches numeric ordering ("5.2" ->
     * "000005.000002", "5.10" -> "000005.000010"). An alphabetic suffix is
     * lowercased and appended. Tokens with no leading digit are lowercased whole.
     *
     * Returns null for null or blank input.
     */
    fun deriveSortKey(ruleNumber: String?): String? {
        if (ruleNumber.isNullOrBlank()) return null
        val normalized = ruleNumber.trim().split('.', '_', '-', '/', ' ').map { token ->
            val digits = token.takeWhile { it.isDigit() }
            if (digits.isEmpty()) {
                token.lowercase()
            } else {
                digits.padStart(6, '0') + token.drop(digits.length).lowercase()
            }
        }
        return normalized.joinToString(".")
    }
}