package eu.gaelicgames.referee.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TeamNameNormalizerTest {

    /**
     * Shared normalization contract. The TypeScript implementation in
     * frontend-vite/src/utils/team_name_normalizer.ts is tested against the
     * exact same vectors. Keep both lists in sync.
     */
    private val vectors = listOf(
        "Zürich" to "zurich",
        "Zurich" to "zurich",
        "ZÜRICH" to "zurich",
        "Saint-Brieuc" to "saint-brieuc",
        "Gaélique Bro Sant Brieg" to "gaelique bro sant brieg",
        "  Padded  Name  " to "padded name",
        "Straßburg" to "strassburg",
        "Ærø" to "aero",
        "Œuvre" to "oeuvre",
        "Þórshöfn" to "thorshofn",
        "Łódź" to "lodz",
        "Ðjurgården" to "djurgarden",
        "" to ""
    )

    @Test
    fun `normalizes shared vectors`() {
        for ((input, expected) in vectors) {
            assertEquals(expected, normalizeTeamName(input), "input was: '$input'")
        }
    }

    @Test
    fun `is idempotent`() {
        for ((input, _) in vectors) {
            val once = normalizeTeamName(input)
            assertEquals(once, normalizeTeamName(once), "input was: '$input'")
        }
    }

    @Test
    fun `does not fold punctuation`() {
        assertEquals("saint-brieuc", normalizeTeamName("Saint-Brieuc"))
        assertEquals("saint brieuc", normalizeTeamName("Saint Brieuc"))
    }
}
