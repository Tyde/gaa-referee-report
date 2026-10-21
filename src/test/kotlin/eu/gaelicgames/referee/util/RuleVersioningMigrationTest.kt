package eu.gaelicgames.referee.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import org.apache.commons.csv.CSVFormat

class RuleVersioningMigrationTest {

    private val ruleNumberRegex = Regex("""Rule\s+([0-9]+(?:\.[0-9]+)?[a-z]?)""", RegexOption.IGNORE_CASE)
    private val rulePrefixRegex = Regex("""^\s*(CAUTION|BLACK CARD|ORDER OFF):\s*Rule\s+[0-9]+(?:\.[0-9]+)?[a-z]?\s*""", RegexOption.IGNORE_CASE)

    @Test
    fun `deriveSortKey produces natural order`() {
        assertNull(RuleSortKeyUtil.deriveSortKey(null))
        assertNull(RuleSortKeyUtil.deriveSortKey(""))
        assertNull(RuleSortKeyUtil.deriveSortKey("   "))
        assertEquals("000005.000002", RuleSortKeyUtil.deriveSortKey("5.2"))
        assertEquals("000005.000010", RuleSortKeyUtil.deriveSortKey("5.10"))
        assertEquals("000005.000034a", RuleSortKeyUtil.deriveSortKey("5.34a"))
        // full ordering: 5.2 < 5.10
        val keys = listOf("5.10", "5.2", "5.34a", "5.3", "6.1").map { RuleSortKeyUtil.deriveSortKey(it)!! }
        val sorted = listOf("5.2", "5.3", "5.10", "5.34a", "6.1").map { RuleSortKeyUtil.deriveSortKey(it)!! }
        assertEquals(sorted, keys.sorted())
        // free-form with suffix like 400g, 400a
        assertNotNull(RuleSortKeyUtil.deriveSortKey("400g"))
        // nulls sort last is handled by SQL, not here
    }

    @Test
    fun `rule number extraction and description stripping`() {
        fun extract(input: String): Pair<String?, String> {
            val parsed = ruleNumberRegex.find(input)?.groupValues?.get(1)
            val stripped = if (parsed != null) input.replaceFirst(rulePrefixRegex, "$1: ") else input
            return parsed to stripped
        }

        val (n1, d1) = extract("CAUTION: Rule 5.3 To threaten or to use abusive ...")
        assertEquals("5.3", n1)
        assertEquals("CAUTION: To threaten or to use abusive ...", d1)

        val (n2, d2) = extract("CAUTION: Rule 400g (Repeat Infraction) Frontal pushing")
        // 400g has no dot but broad regex captures it
        assertEquals("400g", n2)
        assertEquals("CAUTION: (Repeat Infraction) Frontal pushing", d2)

        val (n3, d3) = extract("ORDER OFF: On foot of two cautions as previously stated")
        assertNull(n3)
        assertEquals("ORDER OFF: On foot of two cautions as previously stated", d3)

        val (n4, d4) = extract("CAUTION: Rule 5.11a (Repeat Infraction) To charge ...")
        assertEquals("5.11a", n4)
        assertEquals("CAUTION: (Repeat Infraction) To charge ...", d4) // prefix stripped, leaves "(Repeat…)"
        // Actually current regex strips "CAUTION: Rule 5.11a " -> "CAUTION: " + remainder "(Repeat…)"
        assertTrue(d4.startsWith("CAUTION:"))

        val (n5, d5) = extract("CAUTION: RULE 5 1a.3 - MISCONDUCT AT GAMES ...")
        // malformed "RULE 5 1a.3" — broad regex captures leading "5", stripping leaves partial
        assertEquals("5", n5)
        assertTrue(d5.contains("1a.3"))
    }

    @Test
    fun `CSV export migration covers real data with expected nulls for generic rules`() {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("rules/rules-before-versioning.csv")
            ?: fail("CSV resource not found: rules/rules-before-versioning.csv — ensure file is under src/test/resources/rules/")
        BufferedReader(InputStreamReader(stream)).use { reader ->
            val csv = CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader().setSkipHeaderRecord(true).build()
            val parser = csv.parse(reader)
            var total = 0
            var withNumber = 0
            var withoutNumber = 0
            val unsorted = mutableListOf<Pair<String?, String?>>()
            for (record in parser) {
                total++
                val description = record.get("description")
                val parsed = ruleNumberRegex.find(description)?.groupValues?.get(1)
                val sortKey = parsed?.let { RuleSortKeyUtil.deriveSortKey(it) }
                unsorted.add(parsed to sortKey)
                if (parsed != null) withNumber++ else withoutNumber++
                // sort key sanity
                if (parsed != null) assertNotNull(sortKey, "sortKey should be non-null for $parsed")
            }
            // total rows in export
            assertEquals(176, total, "CSV should have 176 rows (excluding header)")
            // With broad regex, most numbers including 400g are captured; only generic ON FOOT variants stay NULL
            assertTrue(withoutNumber in 3..15, "Expected 3..15 without number, got $withoutNumber (withNumber=$withNumber)")
            assertTrue(withNumber in 160..173, "Expected 160..173 with number, got $withNumber")
            // Natural sort: 5.2 before 5.10
            val sortedNumbers = unsorted.mapNotNull { it.first }.distinct().sortedBy { RuleSortKeyUtil.deriveSortKey(it) }
            val idx2 = sortedNumbers.indexOf("5.2")
            val idx10 = sortedNumbers.indexOf("5.10")
            if (idx2 >= 0 && idx10 >= 0) {
                assertTrue(idx2 < idx10, "5.2 should sort before 5.10, got order $sortedNumbers")
            }
            // description stripping smoke: first row should strip
            val firstDesc = "CAUTION: Rule 5.3 To threaten or to use abusive or provocative language or gestures to an opponent."
            val stripped = firstDesc.replaceFirst(rulePrefixRegex, "$1: ")
            assertEquals("CAUTION: To threaten or to use abusive or provocative language or gestures to an opponent.", stripped)
        }
    }

    @Test
    fun `allRules ordering by sortKey places nulls last`() {
        // Simulate DB ordering: ORDER BY sortKey ASC NULLS LAST, id ASC
        data class Row(val ruleNumber: String?, val id: Long)
        val rows = listOf(
            Row("5.10", 1), Row(null, 2), Row("5.2", 3), Row("5.34a", 4), Row(null, 5)
        )
        val sorted = rows.sortedWith(compareBy<Row> { it.ruleNumber?.let { n -> RuleSortKeyUtil.deriveSortKey(n) } ?: "\uFFFF" }.thenBy { it.id })
        assertEquals(listOf(3L, 1L, 4L, 2L, 5L), sorted.map { it.id })
    }
}
