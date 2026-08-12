# Team Name Aliases Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a team be found under any spelling an international user expects, while every screen keeps displaying the one canonical team name.

**Architecture:** A new `TeamAliases` table holds admin-maintained alternative spellings, one row per spelling, globally unique on a normalized form. Aliases ride along in the existing cached team list payload, so search stays client-side in `TeamSelectField.vue` with no new endpoint. Merge and rename offer to keep the disappearing name as an alias, and a migration backfills aliases from every team already merged away.

**Tech Stack:** Kotlin + Ktor + Exposed (backend), Vue 3 + PrimeVue + Zod (frontend), Vitest (new, frontend unit tests), JUnit 5 (backend tests).

**Spec:** `docs/superpowers/specs/2026-08-11-team-name-aliases-design.md`

## Global Constraints

- **Gradle must run under JDK 21.** Prefix every gradle command with
  `export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home`.
  Under the system JDK 25 the wrapper fails with a cryptic `* What went wrong: 25.0.1`.
- **Frontend uses npm**, not bun, despite `bun.lockb` being present. Run npm commands from `frontend-vite/`.
- **Canonical display only.** No task may change what any report, PDF, stats table or public page renders as a team name. Aliases are search keys.
- **Alias writes are admin-only.** Every new route goes in `AdminApiRouting.kt`, never `RefereeApiRouting.kt` or `PublicApiRouting.kt`.
- **A team's own canonical name is never stored as an alias.** New teams start with zero aliases.
- **`normalized` is derived data.** It is written only by `normalizeTeamName` (Kotlin) / `normalizeForSearch` (TS). The two implementations must stay behaviourally identical; both are covered by the same shared test vectors (Task 1, Task 9).
- **A rejected alias never fails the operation that proposed it.** A merge or rename commits even when its proposed alias is rejected.
- **Every alias mutation calls `CacheUtil.deleteCachedTeamList()`.** The team list is cached; a stale cache silently breaks alias search.

## Refinements to the spec

Two details are decided here, tighter than the spec:

1. **`TeamDEO.aliases` carries objects, not strings** — `List<TeamAliasDEO>?` where `TeamAliasDEO(id, teamId, alias)`. The admin UI needs alias ids to edit and delete individual rows; a bare `List<String>` would force delete-by-text.
2. **Alias matching applies to top-level teams only.** Amalgamation *member* teams inside `amalgamationTeams` do not carry aliases (the joined query that builds them would need a second join, and members are already matched by canonical name). Searching an alias of a member team does not surface the amalgamation.

Normalization deliberately does **not** fold punctuation: `Saint-Brieuc` and `Saint Brieuc` remain distinct aliases. An admin who wants both adds both. Folding hyphens and apostrophes would silently collapse genuinely different club names.

## File Structure

**Backend — create:**
- `src/main/kotlin/eu/gaelicgames/referee/util/TeamNameNormalizer.kt` — the one Kotlin normalizer.
- `src/main/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEO.kt` — all alias mutations and validation.
- `src/test/kotlin/eu/gaelicgames/referee/util/TeamNameNormalizerTest.kt`
- `src/test/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEOTest.kt`

**Backend — modify:**
- `gaa-referee-report-common/src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEOBase.kt` — new DEOs, `TeamDEO.aliases`, `MergeTeamsDEO.aliasesToCreate`, `UpdateTeamDEO`.
- `src/main/kotlin/eu/gaelicgames/referee/data/ReportData.kt` — `TeamAliases` table, `TeamAlias` entity, two `TeamChangeType` values.
- `src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEO.kt` — read paths carry aliases; merge and rename create aliases.
- `src/main/kotlin/eu/gaelicgames/referee/resources/Api.kt` — alias resource paths.
- `src/main/kotlin/eu/gaelicgames/referee/plugins/routing/AdminApiRouting.kt` — alias routes, update route switches DEO.
- `src/main/kotlin/eu/gaelicgames/referee/util/DatabaseUtil.kt` — table registration + Migration 10.
- `src/test/kotlin/eu/gaelicgames/referee/data/api/TeamHistoryDEOTest.kt` — migrate to `UpdateTeamDEO`.

**Frontend — create:**
- `frontend-vite/src/utils/team_name_normalizer.ts` — normalizer, moved out of the component.
- `frontend-vite/src/utils/team_search.ts` — pure scoring/matching, no Vue.
- `frontend-vite/src/utils/__tests__/team_name_normalizer.spec.ts`
- `frontend-vite/src/utils/__tests__/team_search.spec.ts`
- `frontend-vite/vitest.config.ts`
- `frontend-vite/src/components/admin/teams/TeamAliasEditor.vue` — chips + add field, reused by the team list.

**Frontend — modify:**
- `frontend-vite/src/types/team_types.ts`, `frontend-vite/src/utils/api/teams_api.ts`
- `frontend-vite/src/components/team/TeamSelectField.vue`, `frontend-vite/src/components/team/MergeTeamDialog.vue`
- `frontend-vite/src/components/admin/teams/TeamList.vue`, `EditAmalgamationDialog.vue`, `ConvertTeamToAmalgamtionDialog.vue`
- `frontend-vite/src/i18n/edit_report/edit_report_i18n_{en,de,fr,es}.ts`
- `frontend-vite/package.json`

---

### Task 1: Kotlin name normalizer

**Files:**
- Create: `src/main/kotlin/eu/gaelicgames/referee/util/TeamNameNormalizer.kt`
- Test: `src/test/kotlin/eu/gaelicgames/referee/util/TeamNameNormalizerTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `fun normalizeTeamName(input: String): String` in package `eu.gaelicgames.referee.util`.

These test vectors are the contract. Task 9 duplicates them verbatim in TypeScript.

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test --tests "eu.gaelicgames.referee.util.TeamNameNormalizerTest"
```

Expected: compilation failure — `normalizeTeamName` is unresolved.

- [ ] **Step 3: Write minimal implementation**

```kotlin
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
```

- [ ] **Step 4: Run test to verify it passes**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test --tests "eu.gaelicgames.referee.util.TeamNameNormalizerTest"
```

Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/eu/gaelicgames/referee/util/TeamNameNormalizer.kt \
        src/test/kotlin/eu/gaelicgames/referee/util/TeamNameNormalizerTest.kt
git commit -m "feat: add shared team name normalizer"
```

---

### Task 2: TeamAliases table and entity

**Files:**
- Modify: `src/main/kotlin/eu/gaelicgames/referee/data/ReportData.kt:16-50`
- Modify: `src/main/kotlin/eu/gaelicgames/referee/util/DatabaseUtil.kt:80-110` (the `tables` list) and `:148` (migrations block)
- Test: `src/test/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEOTest.kt`

**Interfaces:**
- Consumes: `normalizeTeamName` (Task 1).
- Produces: `object TeamAliases`, `class TeamAlias`, `TeamChangeType.ALIAS_ADDED`, `TeamChangeType.ALIAS_REMOVED`.

- [ ] **Step 1: Write the failing test**

Create `src/test/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEOTest.kt`:

```kotlin
package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.Team
import eu.gaelicgames.referee.data.TeamAlias
import eu.gaelicgames.referee.data.TeamAliases
import eu.gaelicgames.referee.util.normalizeTeamName
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TeamAliasDEOTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp() {
            TestHelper.setupDatabase()
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            TestHelper.tearDownDatabase()
        }
    }

    @Test
    fun `alias row stores normalized form and is globally unique`() {
        runBlocking {
            newSuspendedTransaction {
                val team = Team.new {
                    name = "Table Test Zürich"
                    isAmalgamation = false
                }
                TeamAlias.new {
                    this.team = team
                    alias = "Table Test Zurich"
                    normalized = normalizeTeamName("Table Test Zurich")
                    createdAt = LocalDateTime.now()
                }
                commit()

                val stored = TeamAlias.find {
                    TeamAliases.normalized eq "table test zurich"
                }.first()
                assertEquals("Table Test Zurich", stored.alias)
                assertEquals(team.id, stored.team.id)
            }

            newSuspendedTransaction {
                val otherTeam = Team.new {
                    name = "Table Test Other"
                    isAmalgamation = false
                }
                assertThrows(ExposedSQLException::class.java) {
                    TeamAlias.new {
                        team = otherTeam
                        alias = "Table Test Zürich spelled differently"
                        normalized = "table test zurich"
                        createdAt = LocalDateTime.now()
                    }
                    commit()
                }
            }
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test --tests "eu.gaelicgames.referee.data.api.TeamAliasDEOTest"
```

Expected: compilation failure — `TeamAlias` and `TeamAliases` unresolved.

- [ ] **Step 3: Add the table and entity**

In `ReportData.kt`, directly after the `Amalgamation` class (line 40):

```kotlin
object TeamAliases : LongIdTable() {
    val team = reference("team", Teams)
    val alias = varchar("alias", 100)

    /**
     * Search and uniqueness key, produced by normalizeTeamName().
     * Unique across ALL teams: a spelling must resolve to exactly one team,
     * otherwise search is ambiguous.
     */
    val normalized = varchar("normalized", 100).uniqueIndex()
    val createdAt = datetime("created_at")
    val createdBy = optReference("created_by", Users)
}

class TeamAlias(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<TeamAlias>(TeamAliases)
    var team by Team referencedOn TeamAliases.team
    var alias by TeamAliases.alias
    var normalized by TeamAliases.normalized
    var createdAt by TeamAliases.createdAt
    var createdBy by User optionalReferencedOn TeamAliases.createdBy
}
```

Extend `TeamChangeType` (line 42) with two entries, keeping the existing ones untouched:

```kotlin
    MERGED_INTO("Team merged into another team"),
    ALIAS_ADDED("Alternative spelling added"),
    ALIAS_REMOVED("Alternative spelling removed")
```

- [ ] **Step 4: Register the table and add Migration 10**

In `DatabaseUtil.kt`, add `TeamAliases` to the `tables` list (right after `TeamHistoryEvents`, line 99).

At the end of the migrations block in `createSchema()`, after Migration 9's backfill loop:

```kotlin
            //Migration 10 - Team name aliases
            SchemaUtils.createMissingTablesAndColumns(TeamAliases)
```

The backfill half of Migration 10 is added in Task 8; leave it out for now so this task stays independently testable.

- [ ] **Step 5: Run test to verify it passes**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test --tests "eu.gaelicgames.referee.data.api.TeamAliasDEOTest"
```

Expected: PASS, 1 test. If the unique-constraint assertion fails on SQLite, confirm the index was created — a pre-existing `data/test.db` is deleted by `TestHelper.tearDownDatabase()`, so delete it manually and re-run if the schema looks stale.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/eu/gaelicgames/referee/data/ReportData.kt \
        src/main/kotlin/eu/gaelicgames/referee/util/DatabaseUtil.kt \
        src/test/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEOTest.kt
git commit -m "feat: add TeamAliases table and alias history change types"
```

---

### Task 3: Alias DEOs, validation and mutations

**Files:**
- Modify: `gaa-referee-report-common/src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEOBase.kt`
- Create: `src/main/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEO.kt`
- Test: `src/test/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEOTest.kt` (extend)

**Interfaces:**
- Consumes: `normalizeTeamName` (Task 1), `TeamAlias`/`TeamAliases` (Task 2), `writeTeamHistoryEventInTransaction` (`TeamDEO.kt:19`), `CacheUtil.deleteCachedTeamList()`.
- Produces:
  - `data class TeamAliasDEO(val id: Long, val teamId: Long, val alias: String)`
  - `data class NewTeamAliasDEO(val teamId: Long, val alias: String)`
  - `data class UpdateTeamAliasDEO(val id: Long, val alias: String)`
  - `data class DeleteTeamAliasDEO(val id: Long)`
  - `fun createTeamAliasInTransaction(team: Team, aliasText: String, changeDate: LocalDate, recordedBy: User?): Result<TeamAlias>` — the in-transaction primitive reused by merge (Task 6), rename (Task 7) and backfill (Task 8).
  - `suspend fun NewTeamAliasDEO.createInDatabase(recordedBy: User?): Result<TeamAliasDEO>`
  - `suspend fun UpdateTeamAliasDEO.updateInDatabase(recordedBy: User?): Result<TeamAliasDEO>`
  - `suspend fun DeleteTeamAliasDEO.deleteFromDatabase(recordedBy: User?): Result<DeleteTeamAliasDEO>`
  - `fun aliasesForTeamsInTransaction(teamIds: List<Long>): Map<Long, List<TeamAliasDEO>>`

- [ ] **Step 1: Write the failing tests**

Append to `TeamAliasDEOTest.kt` (inside the class):

```kotlin
    @Test
    fun `create alias succeeds and records history`() {
        runBlocking {
            var teamId = 0L
            newSuspendedTransaction {
                teamId = Team.new {
                    name = "Create Alias FC"
                    isAmalgamation = false
                }.id.value
                commit()
            }

            val result = NewTeamAliasDEO(teamId = teamId, alias = " Créate Alias FC ").createInDatabase()
            assertTrue(result.isSuccess, "expected success, got ${result.exceptionOrNull()?.message}")
            assertEquals("Créate Alias FC", result.getOrThrow().alias)

            newSuspendedTransaction {
                val stored = TeamAlias.findById(result.getOrThrow().id)!!
                assertEquals("create alias fc", stored.normalized)
                val history = TeamHistoryEvent.find {
                    TeamHistoryEvents.team eq teamId
                }.filter { it.changeType == TeamChangeType.ALIAS_ADDED }
                assertEquals(1, history.size)
                assertEquals("Créate Alias FC", history.first().newValue)
            }
        }
    }

    @Test
    fun `create alias rejects duplicate of another alias`() {
        runBlocking {
            var teamAId = 0L
            var teamBId = 0L
            newSuspendedTransaction {
                teamAId = Team.new { name = "Dup Alias A"; isAmalgamation = false }.id.value
                teamBId = Team.new { name = "Dup Alias B"; isAmalgamation = false }.id.value
                commit()
            }
            assertTrue(NewTeamAliasDEO(teamAId, "Shared Spelling").createInDatabase().isSuccess)

            val second = NewTeamAliasDEO(teamBId, "shared  spelling").createInDatabase()
            assertTrue(second.isFailure)
            assertTrue(
                second.exceptionOrNull()!!.message!!.contains("Dup Alias A"),
                "error should name the owning team, was: ${second.exceptionOrNull()?.message}"
            )
        }
    }

    @Test
    fun `create alias rejects collision with any team canonical name`() {
        runBlocking {
            var teamId = 0L
            newSuspendedTransaction {
                teamId = Team.new { name = "Canonical Clash A"; isAmalgamation = false }.id.value
                Team.new { name = "Canonical Clash B"; isAmalgamation = false }
                commit()
            }

            val otherName = NewTeamAliasDEO(teamId, "Canonical Clash B").createInDatabase()
            assertTrue(otherName.isFailure)

            val ownName = NewTeamAliasDEO(teamId, "canonical clash a").createInDatabase()
            assertTrue(ownName.isFailure)
        }
    }

    @Test
    fun `create alias rejects blank input`() {
        runBlocking {
            var teamId = 0L
            newSuspendedTransaction {
                teamId = Team.new { name = "Blank Alias FC"; isAmalgamation = false }.id.value
                commit()
            }
            assertTrue(NewTeamAliasDEO(teamId, "   ").createInDatabase().isFailure)
        }
    }

    @Test
    fun `update and delete alias work and record history`() {
        runBlocking {
            var teamId = 0L
            newSuspendedTransaction {
                teamId = Team.new { name = "Mutable Alias FC"; isAmalgamation = false }.id.value
                commit()
            }
            val created = NewTeamAliasDEO(teamId, "First Spelling").createInDatabase().getOrThrow()

            val updated = UpdateTeamAliasDEO(created.id, "Second Spelling").updateInDatabase().getOrThrow()
            assertEquals("Second Spelling", updated.alias)
            newSuspendedTransaction {
                assertEquals("second spelling", TeamAlias.findById(created.id)!!.normalized)
            }

            assertTrue(DeleteTeamAliasDEO(created.id).deleteFromDatabase().isSuccess)
            newSuspendedTransaction {
                assertNull(TeamAlias.findById(created.id))
                val removedEvents = TeamHistoryEvent.find {
                    TeamHistoryEvents.team eq teamId
                }.filter { it.changeType == TeamChangeType.ALIAS_REMOVED }
                assertEquals(1, removedEvents.size)
                assertEquals("Second Spelling", removedEvents.first().oldValue)
            }
        }
    }
```

Add the imports this needs at the top of the file:

```kotlin
import eu.gaelicgames.referee.data.TeamChangeType
import eu.gaelicgames.referee.data.TeamHistoryEvent
import eu.gaelicgames.referee.data.TeamHistoryEvents
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test --tests "eu.gaelicgames.referee.data.api.TeamAliasDEOTest"
```

Expected: compilation failure — `NewTeamAliasDEO` and friends unresolved.

- [ ] **Step 3: Add the DEOs to the common module**

In `TeamDEOBase.kt`, after `MergeTeamsDEO`:

```kotlin
@Serializable
data class TeamAliasDEO(val id: Long, val teamId: Long, val alias: String)

@Serializable
data class NewTeamAliasDEO(val teamId: Long, val alias: String)

@Serializable
data class UpdateTeamAliasDEO(val id: Long, val alias: String)

@Serializable
data class DeleteTeamAliasDEO(val id: Long)
```

- [ ] **Step 4: Write the implementation**

Create `src/main/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEO.kt`:

```kotlin
package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.lockedTransaction
import eu.gaelicgames.referee.util.normalizeTeamName
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.time.LocalDateTime

fun TeamAlias.toDEO(): TeamAliasDEO = TeamAliasDEO(
    id = id.value,
    teamId = team.id.value,
    alias = alias
)

/**
 * Validates an alias against every other alias and every live team's canonical
 * name. Requires an active transaction. Returns the normalized form on success.
 *
 * @param excludeAliasId alias row being updated, excluded from the collision check
 */
fun validateAliasInTransaction(aliasText: String, excludeAliasId: Long? = null): Result<String> {
    val trimmed = aliasText.trim()
    if (trimmed.isBlank()) {
        return Result.failure(Exception("Alias must not be blank"))
    }
    if (trimmed.length > 100) {
        return Result.failure(Exception("Alias must be at most 100 characters"))
    }
    val normalized = normalizeTeamName(trimmed)

    val clashingAlias = TeamAlias.find { TeamAliases.normalized eq normalized }
        .firstOrNull { it.id.value != excludeAliasId }
    if (clashingAlias != null) {
        return Result.failure(
            Exception("\"$trimmed\" is already an alias of ${clashingAlias.team.name}")
        )
    }

    // Team counts are in the hundreds, so normalizing in memory is cheaper than
    // storing and maintaining a normalized column on Teams.
    val clashingTeam = Team.find { Teams.deletedAt.isNull() }
        .firstOrNull { normalizeTeamName(it.name) == normalized }
    if (clashingTeam != null) {
        return Result.failure(
            Exception("\"$trimmed\" is already the name of team ${clashingTeam.name}")
        )
    }

    return Result.success(normalized)
}

/**
 * Creates an alias inside an existing transaction, writing an ALIAS_ADDED
 * history event. Used directly by merge, rename and the backfill migration so
 * the alias is committed atomically with the change that proposed it.
 */
fun createTeamAliasInTransaction(
    team: Team,
    aliasText: String,
    changeDate: LocalDate,
    recordedBy: User? = null
): Result<TeamAlias> {
    return validateAliasInTransaction(aliasText).map { normalizedValue ->
        val trimmed = aliasText.trim()
        val created = TeamAlias.new {
            this.team = team
            alias = trimmed
            normalized = normalizedValue
            createdAt = LocalDateTime.now()
            createdBy = recordedBy
        }
        writeTeamHistoryEventInTransaction(
            team, TeamChangeType.ALIAS_ADDED, changeDate, null, trimmed, recordedBy
        )
        created
    }
}

suspend fun NewTeamAliasDEO.createInDatabase(recordedBy: User? = null): Result<TeamAliasDEO> {
    CacheUtil.deleteCachedTeamList()
    val deo = this
    return lockedTransaction {
        val team = Team.findById(deo.teamId)
            ?: return@lockedTransaction Result.failure(Exception("Team not found"))
        createTeamAliasInTransaction(team, deo.alias, LocalDate.now(), recordedBy)
            .map { it.toDEO() }
    }
}

suspend fun UpdateTeamAliasDEO.updateInDatabase(recordedBy: User? = null): Result<TeamAliasDEO> {
    CacheUtil.deleteCachedTeamList()
    val deo = this
    return lockedTransaction {
        val existing = TeamAlias.findById(deo.id)
            ?: return@lockedTransaction Result.failure(Exception("Alias not found"))
        validateAliasInTransaction(deo.alias, excludeAliasId = deo.id).map { normalizedValue ->
            val oldText = existing.alias
            val trimmed = deo.alias.trim()
            existing.alias = trimmed
            existing.normalized = normalizedValue
            writeTeamHistoryEventInTransaction(
                existing.team, TeamChangeType.ALIAS_ADDED, LocalDate.now(), oldText, trimmed, recordedBy
            )
            existing.toDEO()
        }
    }
}

suspend fun DeleteTeamAliasDEO.deleteFromDatabase(recordedBy: User? = null): Result<DeleteTeamAliasDEO> {
    CacheUtil.deleteCachedTeamList()
    val deo = this
    return lockedTransaction {
        val existing = TeamAlias.findById(deo.id)
            ?: return@lockedTransaction Result.failure(Exception("Alias not found"))
        writeTeamHistoryEventInTransaction(
            existing.team, TeamChangeType.ALIAS_REMOVED, LocalDate.now(), existing.alias, null, recordedBy
        )
        existing.delete()
        Result.success(DeleteTeamAliasDEO(deo.id))
    }
}

/**
 * Bulk alias lookup for the team list. One query for all teams, grouped by
 * team id, so building the list stays O(1) queries.
 * Requires an active transaction.
 */
fun aliasesForTeamsInTransaction(teamIds: List<Long>): Map<Long, List<TeamAliasDEO>> {
    if (teamIds.isEmpty()) {
        return emptyMap()
    }
    return TeamAliases.selectAll().where { TeamAliases.team inList teamIds }
        .map {
            TeamAliasDEO(
                id = it[TeamAliases.id].value,
                teamId = it[TeamAliases.team].value,
                alias = it[TeamAliases.alias]
            )
        }
        .groupBy { it.teamId }
}
```

Note the `and` / `selectAll` imports may need adjusting to whatever the compiler asks for — `inList` lives in `org.jetbrains.exposed.sql.SqlExpressionBuilder`.

- [ ] **Step 5: Run tests to verify they pass**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test --tests "eu.gaelicgames.referee.data.api.TeamAliasDEOTest"
```

Expected: PASS, 6 tests.

- [ ] **Step 6: Commit**

```bash
git add gaa-referee-report-common/src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEOBase.kt \
        src/main/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEO.kt \
        src/test/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEOTest.kt
git commit -m "feat: add team alias create/update/delete with collision validation"
```

---

### Task 4: Serve aliases in the team list

**Files:**
- Modify: `gaa-referee-report-common/.../TeamDEOBase.kt:12-15` (add `aliases`)
- Modify: `src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEO.kt:60-62` (`fromTeam`), `:136-150` (`allTeamList`)
- Test: `src/test/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEOTest.kt` (extend)

**Interfaces:**
- Consumes: `aliasesForTeamsInTransaction` (Task 3).
- Produces: `TeamDEO.aliases: List<TeamAliasDEO>?`, populated by `allTeamList()` and `fromTeam()`.

`aliases` goes **last** in the constructor with a default of `null`, so the existing positional `TeamDEO(name, id, isAmalgamation, amalgamationTeams)` calls in `TeamDEO.kt`, `TournamentDEO.kt:287` and the tests keep compiling. The `mcp-server` module declares its own `TeamDEO` in `Main.kt:55` and is unaffected.

- [ ] **Step 1: Write the failing test**

Append to `TeamAliasDEOTest.kt`:

```kotlin
    @Test
    fun `all team list carries aliases`() {
        runBlocking {
            var teamId = 0L
            newSuspendedTransaction {
                teamId = Team.new { name = "Listed Alias FC"; isAmalgamation = false }.id.value
                commit()
            }
            NewTeamAliasDEO(teamId, "Listed Spelling One").createInDatabase().getOrThrow()
            NewTeamAliasDEO(teamId, "Listed Spelling Two").createInDatabase().getOrThrow()

            val listed = TeamDEO.allTeamList().first { it.id == teamId }
            assertEquals(
                listOf("Listed Spelling One", "Listed Spelling Two"),
                listed.aliases!!.map { it.alias }.sorted()
            )
        }
    }

    @Test
    fun `team without aliases reports an empty list`() {
        runBlocking {
            var teamId = 0L
            newSuspendedTransaction {
                teamId = Team.new { name = "No Alias FC"; isAmalgamation = false }.id.value
                commit()
            }
            val listed = TeamDEO.allTeamList().first { it.id == teamId }
            assertEquals(emptyList<String>(), listed.aliases!!.map { it.alias })
        }
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test --tests "eu.gaelicgames.referee.data.api.TeamAliasDEOTest"
```

Expected: compilation failure — `aliases` is not a member of `TeamDEO`.

- [ ] **Step 3: Add the field**

In `TeamDEOBase.kt`:

```kotlin
@Serializable
data class TeamDEO(
    val name: String, val id: Long, val isAmalgamation: Boolean, val amalgamationTeams: List<TeamDEO>?,
    @Serializable(with = LocalDateSerializer::class) val changeDate: LocalDate? = null,
    val aliases: List<TeamAliasDEO>? = null
)
```

- [ ] **Step 4: Populate it in the read paths**

In `TeamDEO.kt`, replace `allTeamList()` (line 136):

```kotlin
suspend fun TeamDEO.Companion.allTeamList(): List<TeamDEO> {
    return CacheUtil.getCachedTeamList()
        .getOrElse {
            lockedTransaction {
                val (query, alias) = wrapJoinQuery()
                val dbTeams = mapJoinedResultsToTeamDEO(
                    query.selectAll().where { Teams.deletedAt.isNull() }.toList(),
                    alias
                )
                val aliasesByTeam = aliasesForTeamsInTransaction(dbTeams.map { it.id })
                val withAliases = dbTeams.map { team ->
                    team.copy(aliases = aliasesByTeam[team.id] ?: emptyList())
                }
                CacheUtil.cacheTeamList(withAliases)
                withAliases
            }
        }
}
```

And `fromTeam` (line 60), so single-team responses (create, update, merge) also carry aliases. It runs inside the caller's transaction:

```kotlin
fun TeamDEO.Companion.fromTeam(input: Team, amalgamationTeams: List<TeamDEO>? = null): TeamDEO {
    return TeamDEO(
        input.name,
        input.id.value,
        input.isAmalgamation,
        amalgamationTeams,
        aliases = TeamAlias.find { TeamAliases.team eq input.id }.map { it.toDEO() }
    )
}
```

Leave `wrapRow`, `wrapJoinedRow` and `mapJoinedResultsToTeamDEO` alone: they build amalgamation *member* DEOs, which by design carry no aliases.

- [ ] **Step 5: Run tests to verify they pass**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test --tests "eu.gaelicgames.referee.data.api.*"
```

Expected: PASS. `TeamHistoryDEOTest` and `MergeTeamsDEOTest` must still be green — `fromTeam` gained a field but no caller changed.

- [ ] **Step 6: Commit**

```bash
git add gaa-referee-report-common/src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEOBase.kt \
        src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEO.kt \
        src/test/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEOTest.kt
git commit -m "feat: include team aliases in team list and single-team responses"
```

---

### Task 5: Admin alias routes

**Files:**
- Modify: `src/main/kotlin/eu/gaelicgames/referee/resources/Api.kt:30-46`
- Modify: `src/main/kotlin/eu/gaelicgames/referee/plugins/routing/AdminApiRouting.kt:145-170`

**Interfaces:**
- Consumes: the three alias DEOs and their database functions (Task 3).
- Produces: `POST /api/team/alias/new`, `POST /api/team/alias/update`, `POST /api/team/alias/delete`, all admin-authenticated.

This task has no unit test — the codebase has no routing tests, and the DEO layer beneath is already covered. It is verified by compiling and by a manual request against a running server.

- [ ] **Step 1: Add the resource paths**

In `Api.kt`, inside `class Team(val parent: Api)` next to `Update`, `Merge` and `History`:

```kotlin
        @Serializable
        @Resource("alias")
        class Alias(val parent: Team) {

            @Serializable
            @Resource("new")
            class New(val parent: Alias)

            @Serializable
            @Resource("update")
            class Update(val parent: Alias)

            @Serializable
            @Resource("delete")
            class Delete(val parent: Alias)
        }
```

- [ ] **Step 2: Add the routes**

In `AdminApiRouting.kt`, after the `Api.Team.History` handler (line 170):

```kotlin
    post<Api.Team.Alias.New> {
        receiveAndHandleDEO<NewTeamAliasDEO> { deo ->
            val recordedBy = call.principal<UserPrincipal>()?.user
            deo.createInDatabase(recordedBy).getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.Team.Alias.Update> {
        receiveAndHandleDEO<UpdateTeamAliasDEO> { deo ->
            val recordedBy = call.principal<UserPrincipal>()?.user
            deo.updateInDatabase(recordedBy).getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.Team.Alias.Delete> {
        receiveAndHandleDEO<DeleteTeamAliasDEO> { deo ->
            val recordedBy = call.principal<UserPrincipal>()?.user
            deo.deleteFromDatabase(recordedBy).getOrElse {
                ApiError(ApiErrorOptions.DELETE_FAILED, it.message ?: "Unknown error")
            }
        }
    }
```

If `ApiErrorOptions.DELETE_FAILED` does not exist, use `ApiErrorOptions.INSERTION_FAILED` — check the enum rather than adding a value.

- [ ] **Step 3: Verify it compiles**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew compileKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Verify manually against a running server**

Start the app, log in as an admin in the browser, then from the browser devtools console:

```js
await fetch("/api/team/alias/new", {
  method: "POST",
  headers: {"Content-Type": "application/json"},
  body: JSON.stringify({teamId: 1, alias: "Manual Test Spelling"})
}).then(r => r.json())
```

Expected: a `{id, teamId, alias}` object. Repeat with the same alias to confirm the collision error comes back as an `ApiError`. Delete it afterwards via `/api/team/alias/delete`.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/eu/gaelicgames/referee/resources/Api.kt \
        src/main/kotlin/eu/gaelicgames/referee/plugins/routing/AdminApiRouting.kt
git commit -m "feat: add admin routes for team alias management"
```

---

### Task 6: Merge creates aliases and re-points existing ones

**Files:**
- Modify: `gaa-referee-report-common/.../TeamDEOBase.kt` (`MergeTeamsDEO`)
- Modify: `src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEO.kt:240-367` (`MergeTeamsDEO.updateInDatabase`)
- Test: `src/test/kotlin/eu/gaelicgames/referee/data/api/MergeTeamsDEOTest.kt` (extend)

**Interfaces:**
- Consumes: `createTeamAliasInTransaction` (Task 3).
- Produces: `MergeTeamsDEO.aliasesToCreate: Map<Long, String>` — merged-team-id to alias text. A missing key means the admin denied that alias.

Two behaviours here: aliases the admin asked for, and aliases the merged team already owned, which must follow it to the survivor.

- [ ] **Step 1: Write the failing tests**

Append to `MergeTeamsDEOTest.kt`:

```kotlin
    @Test
    fun `merge creates requested alias and skips denied one`() {
        runBlocking {
            var baseId = 0L
            var keptId = 0L
            var deniedId = 0L
            newSuspendedTransaction {
                baseId = Team.new { name = "Alias Merge Base"; isAmalgamation = false }.id.value
                keptId = Team.new { name = "Alias Merge Kept"; isAmalgamation = false }.id.value
                deniedId = Team.new { name = "Alias Merge Denied"; isAmalgamation = false }.id.value
                commit()
            }

            MergeTeamsDEO(
                baseTeam = baseId,
                teamsToMerge = listOf(keptId, deniedId),
                aliasesToCreate = mapOf(keptId to "Alias Merge Kept Edited")
            ).updateInDatabase().getOrThrow()

            newSuspendedTransaction {
                val aliases = TeamAlias.find { TeamAliases.team eq baseId }.map { it.alias }
                assertEquals(listOf("Alias Merge Kept Edited"), aliases)
            }
        }
    }

    @Test
    fun `merge succeeds even when the proposed alias collides`() {
        runBlocking {
            var baseId = 0L
            var mergedId = 0L
            newSuspendedTransaction {
                baseId = Team.new { name = "Collide Merge Base"; isAmalgamation = false }.id.value
                mergedId = Team.new { name = "Collide Merge Other"; isAmalgamation = false }.id.value
                Team.new { name = "Collide Merge Taken"; isAmalgamation = false }
                commit()
            }

            val result = MergeTeamsDEO(
                baseTeam = baseId,
                teamsToMerge = listOf(mergedId),
                aliasesToCreate = mapOf(mergedId to "Collide Merge Taken")
            ).updateInDatabase()

            assertTrue(result.isSuccess, "merge itself must not fail on a rejected alias")
            newSuspendedTransaction {
                assertTrue(TeamAlias.find { TeamAliases.team eq baseId }.empty())
                assertNotNull(Team.findById(mergedId)!!.mergedInto)
            }
        }
    }

    @Test
    fun `aliases of a merged team follow it to the survivor`() {
        runBlocking {
            var baseId = 0L
            var mergedId = 0L
            newSuspendedTransaction {
                baseId = Team.new { name = "Repoint Base"; isAmalgamation = false }.id.value
                mergedId = Team.new { name = "Repoint Merged"; isAmalgamation = false }.id.value
                commit()
            }
            NewTeamAliasDEO(mergedId, "Repoint Carried Spelling").createInDatabase().getOrThrow()

            MergeTeamsDEO(
                baseTeam = baseId,
                teamsToMerge = listOf(mergedId),
                aliasesToCreate = emptyMap()
            ).updateInDatabase().getOrThrow()

            newSuspendedTransaction {
                val aliases = TeamAlias.find { TeamAliases.team eq baseId }.map { it.alias }
                assertTrue(aliases.contains("Repoint Carried Spelling"))
            }
        }
    }
```

Imports to add: `eu.gaelicgames.referee.data.TeamAlias`, `eu.gaelicgames.referee.data.TeamAliases`, `org.junit.jupiter.api.Assertions.assertEquals`, `assertTrue`, `assertNotNull`.

- [ ] **Step 2: Run tests to verify they fail**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test --tests "eu.gaelicgames.referee.data.api.MergeTeamsDEOTest"
```

Expected: compilation failure — `MergeTeamsDEO` has no `aliasesToCreate`.

- [ ] **Step 3: Extend the DEO**

In `TeamDEOBase.kt`:

```kotlin
@Serializable
data class MergeTeamsDEO(
    val baseTeam: Long, val teamsToMerge: List<Long>,
    @Serializable(with = LocalDateSerializer::class) val changeDate: LocalDate? = null,
    /** merged-team-id -> alias text. A missing entry means the admin denied that alias. */
    val aliasesToCreate: Map<Long, String> = emptyMap()
)
```

- [ ] **Step 4: Apply aliases during the merge**

In `MergeTeamsDEO.updateInDatabase` (`TeamDEO.kt:240`), inside the `teamsToMerge.forEach` block, immediately before the existing `//History + soft-delete instead of hard delete` comment at line 352:

```kotlin
                    //Aliases owned by the merged team follow it to the survivor.
                    //Collisions are dropped rather than failing the merge.
                    TeamAlias.find { TeamAliases.team eq mergeTeam.id }.toList().forEach { existingAlias ->
                        val stillFree = validateAliasInTransaction(
                            existingAlias.alias, excludeAliasId = existingAlias.id.value
                        ).isSuccess
                        if (stillFree) {
                            existingAlias.team = team
                        } else {
                            existingAlias.delete()
                        }
                    }

                    //The admin may have asked to keep the merged team's name as an alias.
                    //A rejected alias must never fail the merge.
                    aliasesToCreate[mergeTeamId]?.let { requestedAlias ->
                        createTeamAliasInTransaction(team, requestedAlias, changeDate, recordedBy)
                    }
```

Order matters: re-pointing runs before the soft-delete so `mergeTeam` still resolves, and the requested alias is created after re-pointing so an identical carried alias wins the unique index and the duplicate is simply rejected.

- [ ] **Step 5: Run tests to verify they pass**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test --tests "eu.gaelicgames.referee.data.api.MergeTeamsDEOTest"
```

Expected: PASS, including the pre-existing merge tests.

- [ ] **Step 6: Commit**

```bash
git add gaa-referee-report-common/src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEOBase.kt \
        src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEO.kt \
        src/test/kotlin/eu/gaelicgames/referee/data/api/MergeTeamsDEOTest.kt
git commit -m "feat: merge keeps the merged team's name and aliases as search aliases"
```

---

### Task 7: Rename can keep the old name as an alias

**Files:**
- Modify: `gaa-referee-report-common/.../TeamDEOBase.kt` (add `UpdateTeamDEO`)
- Modify: `src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEO.kt:163-238`
- Modify: `src/main/kotlin/eu/gaelicgames/referee/plugins/routing/AdminApiRouting.kt:145-154`
- Modify: `src/test/kotlin/eu/gaelicgames/referee/data/api/TeamHistoryDEOTest.kt`
- Test: `src/test/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEOTest.kt` (extend)

**Interfaces:**
- Consumes: `createTeamAliasInTransaction` (Task 3).
- Produces: `data class UpdateTeamDEO(name, id, isAmalgamation, amalgamationTeams, changeDate, keepOldNameAsAlias)` and `suspend fun UpdateTeamDEO.updateInDatabase(recordedBy: User?): Result<Team>`.

`TeamDEO.updateInDatabase` is **replaced** by `UpdateTeamDEO.updateInDatabase`, not duplicated. `TeamDEO` stays the read shape. The wire format is a superset of what clients send today (`name`, `id`, `isAmalgamation`, `amalgamationTeams`, `changeDate`), so the mcp-server's `updateTeam` call keeps working untouched.

- [ ] **Step 1: Write the failing test**

Append to `TeamAliasDEOTest.kt`:

```kotlin
    @Test
    fun `rename keeps the old name as an alias when requested`() {
        runBlocking {
            var teamId = 0L
            newSuspendedTransaction {
                teamId = Team.new { name = "Old Rename Name"; isAmalgamation = false }.id.value
                commit()
            }

            UpdateTeamDEO(
                name = "New Rename Name",
                id = teamId,
                isAmalgamation = false,
                amalgamationTeams = null,
                keepOldNameAsAlias = "Old Rename Name"
            ).updateInDatabase().getOrThrow()

            newSuspendedTransaction {
                assertEquals("New Rename Name", Team.findById(teamId)!!.name)
                assertEquals(
                    listOf("Old Rename Name"),
                    TeamAlias.find { TeamAliases.team eq teamId }.map { it.alias }
                )
            }
        }
    }

    @Test
    fun `rename without the flag creates no alias`() {
        runBlocking {
            var teamId = 0L
            newSuspendedTransaction {
                teamId = Team.new { name = "Silent Rename Old"; isAmalgamation = false }.id.value
                commit()
            }

            UpdateTeamDEO(
                name = "Silent Rename New",
                id = teamId,
                isAmalgamation = false,
                amalgamationTeams = null
            ).updateInDatabase().getOrThrow()

            newSuspendedTransaction {
                assertTrue(TeamAlias.find { TeamAliases.team eq teamId }.empty())
            }
        }
    }

    @Test
    fun `rename succeeds even when the proposed alias collides`() {
        runBlocking {
            var teamId = 0L
            newSuspendedTransaction {
                teamId = Team.new { name = "Clash Rename Old"; isAmalgamation = false }.id.value
                Team.new { name = "Clash Rename Taken"; isAmalgamation = false }
                commit()
            }

            val result = UpdateTeamDEO(
                name = "Clash Rename New",
                id = teamId,
                isAmalgamation = false,
                amalgamationTeams = null,
                keepOldNameAsAlias = "Clash Rename Taken"
            ).updateInDatabase()

            assertTrue(result.isSuccess)
            newSuspendedTransaction {
                assertEquals("Clash Rename New", Team.findById(teamId)!!.name)
                assertTrue(TeamAlias.find { TeamAliases.team eq teamId }.empty())
            }
        }
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test --tests "eu.gaelicgames.referee.data.api.TeamAliasDEOTest"
```

Expected: compilation failure — `UpdateTeamDEO` unresolved.

- [ ] **Step 3: Add the write DEO**

In `TeamDEOBase.kt`, after `NewTeamDEO`:

```kotlin
/**
 * Write shape for the team update endpoint. Kept separate from TeamDEO so the
 * read shape carries no write-only fields.
 */
@Serializable
data class UpdateTeamDEO(
    val name: String,
    val id: Long,
    val isAmalgamation: Boolean,
    val amalgamationTeams: List<TeamDEO>?,
    @Serializable(with = LocalDateSerializer::class) val changeDate: LocalDate? = null,
    /** Old name to keep as a search alias after a rename. null means don't. */
    val keepOldNameAsAlias: String? = null
)
```

- [ ] **Step 4: Move the update logic onto it**

In `TeamDEO.kt`, change the receiver at line 163 from `TeamDEO` to `UpdateTeamDEO`:

```kotlin
suspend fun UpdateTeamDEO.updateInDatabase(recordedBy: User? = null): Result<Team> {
```

The body is unchanged except for the history block at line 205. Replace it with:

```kotlin
            // ---- history ----
            if (oldName != team.name) {
                writeTeamHistoryEventInTransaction(
                    team, TeamChangeType.RENAMED, changeDate, oldName, team.name, recordedBy
                )
                //The admin may have asked to keep the old name findable.
                //A rejected alias must never fail the rename.
                thisTeam.keepOldNameAsAlias?.let { requestedAlias ->
                    createTeamAliasInTransaction(team, requestedAlias, changeDate, recordedBy)
                }
            }
```

The alias is only created when the name actually changed — a save that only edits amalgamation membership must not produce one.

- [ ] **Step 5: Switch the route and the existing tests**

In `AdminApiRouting.kt:146`, change `receiveAndHandleDEO<TeamDEO>` to `receiveAndHandleDEO<UpdateTeamDEO>`.

In `TeamHistoryDEOTest.kt`, the four `TeamDEO(...)` calls at lines 70, 102, 145 and 232 that are used as update payloads become `UpdateTeamDEO(...)` with the same arguments. The `TeamDEO("Member Team", memberId, false, null)` at line 107 is a nested amalgamation member and stays a `TeamDEO`.

- [ ] **Step 6: Run the full backend suite**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test
```

Expected: PASS, all modules.

- [ ] **Step 7: Commit**

```bash
git add gaa-referee-report-common/src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEOBase.kt \
        src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEO.kt \
        src/main/kotlin/eu/gaelicgames/referee/plugins/routing/AdminApiRouting.kt \
        src/test/kotlin/eu/gaelicgames/referee/data/api/TeamHistoryDEOTest.kt \
        src/test/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEOTest.kt
git commit -m "feat: rename can keep the old team name as a search alias"
```

---

### Task 8: Backfill aliases from already-merged teams

**Files:**
- Modify: `src/main/kotlin/eu/gaelicgames/referee/util/DatabaseUtil.kt` (Migration 10, added in Task 2)
- Test: `src/test/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEOTest.kt` (extend)

**Interfaces:**
- Consumes: `createTeamAliasInTransaction` (Task 3), `normalizeTeamName` (Task 1).
- Produces: `fun backfillAliasesFromMergedTeams(): Int` in `eu.gaelicgames.referee.util`, returning how many aliases it created. Callable from the migration and directly from tests.

The migration is idempotent: it only creates aliases that don't exist, so re-running it on an already-migrated database is a no-op.

- [ ] **Step 1: Write the failing test**

Append to `TeamAliasDEOTest.kt`:

```kotlin
    @Test
    fun `backfill walks the mergedInto chain to the surviving team`() {
        runBlocking {
            var survivorId = 0L
            var firstDeadId = 0L
            var secondDeadId = 0L
            newSuspendedTransaction {
                val survivor = Team.new { name = "Backfill Survivor"; isAmalgamation = false }
                val firstDead = Team.new { name = "Backfill Dead One"; isAmalgamation = false }
                val secondDead = Team.new { name = "Backfill Dead Two"; isAmalgamation = false }
                survivorId = survivor.id.value
                firstDeadId = firstDead.id.value
                secondDeadId = secondDead.id.value

                //secondDead -> firstDead -> survivor
                firstDead.deletedAt = LocalDateTime.now()
                firstDead.mergedInto = survivor
                secondDead.deletedAt = LocalDateTime.now()
                secondDead.mergedInto = firstDead
                commit()
            }

            val created = backfillAliasesFromMergedTeams()
            assertTrue(created >= 2, "expected at least two aliases, got $created")

            newSuspendedTransaction {
                val aliases = TeamAlias.find { TeamAliases.team eq survivorId }.map { it.alias }
                assertTrue(aliases.contains("Backfill Dead One"), "aliases were $aliases")
                assertTrue(aliases.contains("Backfill Dead Two"), "aliases were $aliases")
            }

            //Idempotent: a second run creates nothing new
            assertEquals(0, backfillAliasesFromMergedTeams())
        }
    }
```

Add `import eu.gaelicgames.referee.util.backfillAliasesFromMergedTeams` and `import java.time.LocalDateTime`.

- [ ] **Step 2: Run test to verify it fails**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test --tests "eu.gaelicgames.referee.data.api.TeamAliasDEOTest"
```

Expected: compilation failure — `backfillAliasesFromMergedTeams` unresolved.

- [ ] **Step 3: Write the backfill**

Create `src/main/kotlin/eu/gaelicgames/referee/util/TeamAliasBackfill.kt`:

```kotlin
package eu.gaelicgames.referee.util

import eu.gaelicgames.referee.data.Team
import eu.gaelicgames.referee.data.Teams
import eu.gaelicgames.referee.data.api.createTeamAliasInTransaction
import java.time.LocalDate

/**
 * Turns every already-merged team's name into a search alias of the team that
 * survived the merge, so the alias set is useful on day one instead of waiting
 * for an admin to type it all in.
 *
 * Idempotent: aliases that already exist are rejected by the collision check
 * and silently skipped. Returns the number of aliases created.
 */
suspend fun backfillAliasesFromMergedTeams(): Int {
    return lockedTransaction {
        var created = 0
        val mergedTeams = Team.find { Teams.mergedInto.isNotNull() }.toList()
        for (deadTeam in mergedTeams) {
            val survivor = resolveSurvivor(deadTeam) ?: continue
            if (normalizeTeamName(survivor.name) == normalizeTeamName(deadTeam.name)) {
                continue
            }
            val result = createTeamAliasInTransaction(
                survivor, deadTeam.name, LocalDate.now(), null
            )
            if (result.isSuccess) {
                created++
            }
        }
        created
    }
}

/**
 * Follows the mergedInto chain to the team that is still live.
 * Returns null on a cycle or a chain that ends in another dead team.
 */
private fun resolveSurvivor(start: Team): Team? {
    var current: Team = start.mergedInto ?: return null
    val seen = mutableSetOf(start.id.value)
    while (true) {
        if (!seen.add(current.id.value)) {
            return null
        }
        val next = current.mergedInto ?: return if (current.deletedAt == null) current else null
        current = next
    }
}
```

- [ ] **Step 4: Call it from Migration 10**

In `DatabaseUtil.kt`, extend the Migration 10 block added in Task 2:

```kotlin
            //Migration 10 - Team name aliases
            SchemaUtils.createMissingTablesAndColumns(TeamAliases)
```

and, after the `lockedTransaction { }` block that wraps `createSchema()`'s migrations, add the backfill as its own call so it runs in a fresh transaction:

```kotlin
        //Migration 10 backfill: every team already merged away becomes a
        //search alias of the team that survived the merge.
        backfillAliasesFromMergedTeams()
```

Renames are deliberately **not** backfilled from `TeamHistoryEvents`: historic renames include typo corrections, and those must not become permanent search keys.

- [ ] **Step 5: Run tests to verify they pass**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test
```

Expected: PASS. Note that `TestHelper.setupDatabase()` calls `createSchema()`, so the backfill now runs in every test — it must be a no-op on an empty database.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/eu/gaelicgames/referee/util/TeamAliasBackfill.kt \
        src/main/kotlin/eu/gaelicgames/referee/util/DatabaseUtil.kt \
        src/test/kotlin/eu/gaelicgames/referee/data/api/TeamAliasDEOTest.kt
git commit -m "feat: backfill search aliases from previously merged teams"
```

---

### Task 9: Frontend test runner, normalizer and search scoring

**Files:**
- Modify: `frontend-vite/package.json`
- Create: `frontend-vite/vitest.config.ts`
- Create: `frontend-vite/src/utils/team_name_normalizer.ts`
- Create: `frontend-vite/src/utils/team_search.ts`
- Create: `frontend-vite/src/utils/__tests__/team_name_normalizer.spec.ts`
- Create: `frontend-vite/src/utils/__tests__/team_search.spec.ts`
- Modify: `frontend-vite/src/types/team_types.ts`

**Interfaces:**
- Consumes: the normalization contract from Task 1, `TeamDEO.aliases` from Task 4.
- Produces:
  - `normalizeForSearch(str: string): string`
  - `interface TeamAliasDEO { id: number, teamId: number, alias: string }` and `Team.aliases?: TeamAliasDEO[] | null`
  - `interface TeamSearchResult { team: Team, score: number, matchedAlias?: string }`
  - `searchTeams(teams: Team[], searchTerm: string): TeamSearchResult[]`

The frontend has no unit test runner today (only an empty Cypress scaffold), so this task adds Vitest. The search scoring is real logic and must not ship untested.

- [ ] **Step 1: Install Vitest and add the script**

```bash
cd frontend-vite
npm install -D vitest
```

Add to the `scripts` block in `package.json`:

```json
    "test:unit": "vitest run",
```

Create `frontend-vite/vitest.config.ts`:

```ts
import {fileURLToPath} from "node:url";
import {defineConfig} from "vitest/config";

export default defineConfig({
    resolve: {
        alias: {
            "@": fileURLToPath(new URL("./src", import.meta.url))
        }
    },
    test: {
        environment: "node",
        include: ["src/**/__tests__/*.spec.ts"]
    }
});
```

- [ ] **Step 2: Write the failing normalizer test**

Create `frontend-vite/src/utils/__tests__/team_name_normalizer.spec.ts`:

```ts
import {describe, expect, it} from "vitest";
import {normalizeForSearch} from "@/utils/team_name_normalizer";

/**
 * Shared normalization contract. The Kotlin implementation in
 * src/main/kotlin/eu/gaelicgames/referee/util/TeamNameNormalizer.kt is tested
 * against the exact same vectors. Keep both lists in sync.
 */
const vectors: Array<[string, string]> = [
    ["Zürich", "zurich"],
    ["Zurich", "zurich"],
    ["ZÜRICH", "zurich"],
    ["Saint-Brieuc", "saint-brieuc"],
    ["Gaélique Bro Sant Brieg", "gaelique bro sant brieg"],
    ["  Padded  Name  ", "padded name"],
    ["Straßburg", "strassburg"],
    ["Ærø", "aero"],
    ["Œuvre", "oeuvre"],
    ["Þórshöfn", "thorshofn"],
    ["Łódź", "lodz"],
    ["Ðjurgården", "djurgarden"],
    ["", ""]
];

describe("normalizeForSearch", () => {
    it("normalizes shared vectors", () => {
        for (const [input, expected] of vectors) {
            expect(normalizeForSearch(input), `input was: '${input}'`).toBe(expected);
        }
    });

    it("is idempotent", () => {
        for (const [input] of vectors) {
            const once = normalizeForSearch(input);
            expect(normalizeForSearch(once)).toBe(once);
        }
    });

    it("does not fold punctuation", () => {
        expect(normalizeForSearch("Saint-Brieuc")).toBe("saint-brieuc");
        expect(normalizeForSearch("Saint Brieuc")).toBe("saint brieuc");
    });
});
```

- [ ] **Step 3: Run it to verify it fails**

```bash
cd frontend-vite && npm run test:unit
```

Expected: FAIL — cannot resolve `@/utils/team_name_normalizer`.

- [ ] **Step 4: Write the normalizer**

Create `frontend-vite/src/utils/team_name_normalizer.ts`, lifted from `TeamSelectField.vue:140` with three changes: `toLowerCase` instead of `toLocaleLowerCase` (locale-dependent casing would disagree with the backend under a Turkish locale), whitespace collapsing, and trimming.

```ts
/**
 * Characters that carry no combining mark and therefore survive NFD
 * decomposition, so they need an explicit expansion.
 * Mirrors EXTRA_EXPANSIONS in
 * src/main/kotlin/eu/gaelicgames/referee/util/TeamNameNormalizer.kt.
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
 * collapsed. Punctuation is deliberately preserved, so "Saint-Brieuc" and
 * "Saint Brieuc" stay distinct.
 *
 * Must stay behaviourally identical to normalizeTeamName() on the backend —
 * that function decides which aliases are allowed to exist.
 */
export function normalizeForSearch(str: string): string {
    return str
        .toLowerCase()
        .replace(/[ßæœøđðþł]/g, char => extraMap[char] || char)
        .normalize("NFD")
        .replace(/\p{Diacritic}/gu, "")
        .replace(/\s+/g, " ")
        .trim();
}
```

- [ ] **Step 5: Run it to verify it passes**

```bash
cd frontend-vite && npm run test:unit
```

Expected: PASS, 3 tests.

- [ ] **Step 6: Add the alias type**

In `frontend-vite/src/types/team_types.ts`, add the alias DEO and extend `Team`:

```ts
export interface TeamAliasDEO {
    id: number,
    teamId: number,
    alias: string,
}

export const TeamAliasDEO: z.ZodType<TeamAliasDEO> = z.object({
    id: z.number(),
    teamId: z.number(),
    alias: z.string().min(1),
})
```

```ts
export interface Team {
    name: string,
    id: number,
    isAmalgamation: boolean,
    amalgamationTeams?: Team[] | null,
    changeDate?: string | null,
    aliases?: TeamAliasDEO[] | null,
}

export const Team: z.ZodType<Team> = z.lazy(() =>
    z.object({
        name: z.string().min(1),
        id: z.number(),
        isAmalgamation: z.boolean(),
        amalgamationTeams: Team.array().optional().nullable(),
        changeDate: z.string().optional().nullable(),
        aliases: TeamAliasDEO.array().optional().nullable()
    })
);
```

Extend `MergeTeamsDEO` in the same file:

```ts
export const MergeTeamsDEO = z.object({
    baseTeam: z.number(),
    teamsToMerge: z.array(z.number()),
    changeDate: z.string().optional().nullable(),
    aliasesToCreate: z.record(z.string(), z.string()).optional(),
})
```

- [ ] **Step 7: Write the failing search test**

Create `frontend-vite/src/utils/__tests__/team_search.spec.ts`:

```ts
import {describe, expect, it} from "vitest";
import type {Team} from "@/types/team_types";
import {searchTeams} from "@/utils/team_search";

function team(id: number, name: string, aliases: string[] = []): Team {
    return {
        id,
        name,
        isAmalgamation: false,
        amalgamationTeams: null,
        aliases: aliases.map((alias, index) => ({id: id * 100 + index, teamId: id, alias}))
    };
}

const zurich = team(1, "Zürich Inneractive", ["Zurich GAA"]);
const brieuc = team(2, "Gaélique Bro Sant Brieg", ["Saint-Brieuc"]);
const zug = team(3, "Zug Wanderers");
const amalgamation: Team = {
    id: 4,
    name: "Swiss Selection",
    isAmalgamation: true,
    amalgamationTeams: [team(1, "Zürich Inneractive"), team(3, "Zug Wanderers")],
    aliases: []
};
const allTeams = [zurich, brieuc, zug, amalgamation];

describe("searchTeams", () => {
    it("returns every team sorted by name when the term is empty", () => {
        const results = searchTeams(allTeams, "");
        expect(results.map(r => r.team.name)).toEqual([
            "Gaélique Bro Sant Brieg",
            "Swiss Selection",
            "Zug Wanderers",
            "Zürich Inneractive"
        ]);
    });

    it("finds a team by an alias and reports which alias matched", () => {
        const results = searchTeams(allTeams, "Saint-Brieuc");
        expect(results).toHaveLength(1);
        expect(results[0].team.id).toBe(2);
        expect(results[0].matchedAlias).toBe("Saint-Brieuc");
    });

    it("does not report a matched alias when the canonical name also matched", () => {
        const results = searchTeams(allTeams, "Zürich");
        expect(results[0].team.id).toBe(1);
        expect(results[0].matchedAlias).toBeUndefined();
    });

    it("ranks canonical matches above alias matches", () => {
        const aliasOnly = team(5, "Completely Different", ["Zug"]);
        const results = searchTeams([aliasOnly, zug], "Zug");
        expect(results.map(r => r.team.id)).toEqual([3, 5]);
    });

    it("ranks exact above prefix above substring", () => {
        const exact = team(6, "Cork");
        const prefix = team(7, "Cork Harlequins");
        const substring = team(8, "New Cork");
        const results = searchTeams([substring, prefix, exact], "Cork");
        expect(results.map(r => r.team.id)).toEqual([6, 7, 8]);
    });

    it("matches amalgamation member names", () => {
        const results = searchTeams([amalgamation], "Wanderers");
        expect(results).toHaveLength(1);
        expect(results[0].team.id).toBe(4);
    });

    it("ignores diacritic differences in the search term", () => {
        expect(searchTeams(allTeams, "zurich").map(r => r.team.id)).toContain(1);
    });

    it("returns nothing when no team matches", () => {
        expect(searchTeams(allTeams, "Ballinasloe")).toHaveLength(0);
    });
});
```

- [ ] **Step 8: Run it to verify it fails**

```bash
cd frontend-vite && npm run test:unit
```

Expected: FAIL — cannot resolve `@/utils/team_search`.

- [ ] **Step 9: Write the search module**

Create `frontend-vite/src/utils/team_search.ts`:

```ts
import type {Team} from "@/types/team_types";
import {normalizeForSearch} from "@/utils/team_name_normalizer";

export interface TeamSearchResult {
    team: Team,
    score: number,
    /** Set only when the team was found via an alias and NOT via its canonical name. */
    matchedAlias?: string
}

const SCORE_CANONICAL_EXACT = 100;
const SCORE_CANONICAL_PREFIX = 80;
const SCORE_CANONICAL_SUBSTRING = 60;
const SCORE_ALIAS_EXACT = 50;
const SCORE_ALIAS_PREFIX = 40;
const SCORE_ALIAS_SUBSTRING = 30;
const SCORE_MEMBER = 20;

function scoreText(candidate: string, normalizedTerm: string, exact: number, prefix: number, substring: number): number {
    const normalizedCandidate = normalizeForSearch(candidate);
    if (normalizedCandidate === normalizedTerm) {
        return exact;
    }
    if (normalizedCandidate.startsWith(normalizedTerm)) {
        return prefix;
    }
    if (normalizedCandidate.includes(normalizedTerm)) {
        return substring;
    }
    return 0;
}

/**
 * Ranks teams against a search term. Canonical names always outrank aliases, so
 * familiar results stay on top and aliases only fill in what would otherwise be
 * an empty list.
 */
export function searchTeams(teams: Team[], searchTerm: string): TeamSearchResult[] {
    const normalizedTerm = normalizeForSearch(searchTerm);

    if (!normalizedTerm) {
        return [...teams]
            .sort((a, b) => a.name.localeCompare(b.name))
            .map(team => ({team, score: 0}));
    }

    const results: TeamSearchResult[] = [];
    for (const team of teams) {
        const canonicalScore = scoreText(
            team.name, normalizedTerm,
            SCORE_CANONICAL_EXACT, SCORE_CANONICAL_PREFIX, SCORE_CANONICAL_SUBSTRING
        );

        let aliasScore = 0;
        let matchedAlias: string | undefined = undefined;
        for (const alias of team.aliases ?? []) {
            const score = scoreText(
                alias.alias, normalizedTerm,
                SCORE_ALIAS_EXACT, SCORE_ALIAS_PREFIX, SCORE_ALIAS_SUBSTRING
            );
            if (score > aliasScore) {
                aliasScore = score;
                matchedAlias = alias.alias;
            }
        }

        const memberScore = (team.amalgamationTeams ?? []).some(
            member => normalizeForSearch(member.name).includes(normalizedTerm)
        ) ? SCORE_MEMBER : 0;

        const score = Math.max(canonicalScore, aliasScore, memberScore);
        if (score > 0) {
            results.push({
                team,
                score,
                //Only surface the alias when it is the reason this team is visible.
                matchedAlias: canonicalScore > 0 ? undefined : matchedAlias
            });
        }
    }

    return results.sort((a, b) => b.score - a.score || a.team.name.localeCompare(b.team.name));
}
```

- [ ] **Step 10: Run tests to verify they pass**

```bash
cd frontend-vite && npm run test:unit && npm run type-check
```

Expected: PASS, 11 tests, and a clean type-check.

- [ ] **Step 11: Commit**

```bash
git add frontend-vite/package.json frontend-vite/package-lock.json \
        frontend-vite/vitest.config.ts \
        frontend-vite/src/utils/team_name_normalizer.ts \
        frontend-vite/src/utils/team_search.ts \
        frontend-vite/src/utils/__tests__ \
        frontend-vite/src/types/team_types.ts
git commit -m "feat: add vitest, shared normalizer and alias-aware team search scoring"
```

---

### Task 10: Team picker uses alias search

**Files:**
- Modify: `frontend-vite/src/components/team/TeamSelectField.vue:10-13, 140-157, 163-221, 299-320`
- Modify: `frontend-vite/src/i18n/edit_report/edit_report_i18n_{en,de,fr,es}.ts` (the `teamSelect` block)

**Interfaces:**
- Consumes: `searchTeams`, `TeamSearchResult` (Task 9).
- Produces: no exports; this is the user-facing payoff.

- [ ] **Step 1: Replace the local matching with the shared module**

In the `<script setup>` block, delete the local `SearchResultTeam` interface (lines 10-13) and the `normalizeForSearch` function (lines 140-157), and import instead:

```ts
import {searchTeams, type TeamSearchResult} from "@/utils/team_search";
```

- [ ] **Step 2: Rewrite `filtered_list`**

Keep every existing filter (squads, teams, amalgamations, forcefully hidden, exclude list) exactly as it is, and replace only the trailing search block (lines 201-221) with:

```ts
  return searchTeams(preparedlist, searchTerm.value)
```

The computed's return type becomes `TeamSearchResult[]`.

- [ ] **Step 3: Fix the list key and show the matched alias**

At line 309, replace `:key="srt.search_score"` with `:key="srt.team.id"`. The old key was identical for every row.

Inside the non-amalgamation branch, directly under `<div>{{ srt.team.name }}</div>` (line 336):

```html
                <div v-if="srt.matchedAlias" class="matched-alias-subtitle">
                  {{ $t('teamSelect.matchedAlias', {alias: srt.matchedAlias}) }}
                </div>
```

And in the amalgamation branch, after the name line (line 322):

```html
              <p v-if="srt.matchedAlias" class="matched-alias-subtitle">
                {{ $t('teamSelect.matchedAlias', {alias: srt.matchedAlias}) }}
              </p>
```

Add to the scoped styles:

```css
.matched-alias-subtitle {
  @apply text-sm italic opacity-80;
}
```

- [ ] **Step 4: Add the translation key**

In each of the four `edit_report_i18n_*.ts` files, add to the `teamSelect` block:

- `en`: `matchedAlias: 'also known as "{alias}"',`
- `de`: `matchedAlias: 'auch bekannt als "{alias}"',`
- `fr`: `matchedAlias: 'aussi appelé "{alias}"',`
- `es`: `matchedAlias: 'también conocido como "{alias}"',`

Match the quoting and trailing-comma style already used by the neighbouring keys in each file.

- [ ] **Step 5: Verify**

```bash
cd frontend-vite && npm run type-check && npm run test:unit
```

Expected: clean type-check, tests still passing.

Then start the app, open a report's team selector, and confirm: searching a canonical name works as before; searching an alias surfaces the team with the "also known as" line; the alias line does **not** appear when the canonical name matched.

- [ ] **Step 6: Commit**

```bash
git add frontend-vite/src/components/team/TeamSelectField.vue frontend-vite/src/i18n
git commit -m "feat: team picker finds teams by alternative spellings"
```

---

### Task 11: Alias API client and admin alias editor

**Files:**
- Modify: `frontend-vite/src/utils/api/teams_api.ts`
- Create: `frontend-vite/src/components/admin/teams/TeamAliasEditor.vue`
- Modify: `frontend-vite/src/components/admin/teams/TeamList.vue`

**Interfaces:**
- Consumes: the alias routes (Task 5), `TeamAliasDEO` (Task 9).
- Produces:
  - `addTeamAlias(teamId: number, alias: string): Promise<TeamAliasDEO>`
  - `updateTeamAlias(id: number, alias: string): Promise<TeamAliasDEO>`
  - `deleteTeamAlias(id: number): Promise<{id: number}>`
  - `<TeamAliasEditor :team="team" @aliases-changed="() => ..." />`

- [ ] **Step 1: Add the API functions**

In `teams_api.ts`, importing `TeamAliasDEO` from `@/types/team_types`:

```ts
export async function addTeamAlias(teamId: number, alias: string): Promise<TeamAliasDEO> {
    return makePostRequest("/api/team/alias/new", {teamId: teamId, alias: alias.trim()})
        .then(data => parseAndHandleDEO(data, TeamAliasDEO))
}

export async function updateTeamAlias(id: number, alias: string): Promise<TeamAliasDEO> {
    return makePostRequest("/api/team/alias/update", {id: id, alias: alias.trim()})
        .then(data => parseAndHandleDEO(data, TeamAliasDEO))
}

export async function deleteTeamAlias(id: number): Promise<{ id: number }> {
    return makePostRequest("/api/team/alias/delete", {id: id})
        .then(data => parseAndHandleDEO(data, DeletedTeamAliasDEO))
}
```

Add the response schema to `team_types.ts`:

```ts
export const DeletedTeamAliasDEO = z.object({id: z.number()})
export type DeletedTeamAliasDEO = z.infer<typeof DeletedTeamAliasDEO>;
```

and import it in `teams_api.ts`.

- [ ] **Step 2: Build the editor component**

Create `frontend-vite/src/components/admin/teams/TeamAliasEditor.vue`:

```vue
<script lang="ts" setup>
import {ref} from "vue";
import type {Team} from "@/types/team_types";
import {addTeamAlias, deleteTeamAlias} from "@/utils/api/teams_api";

const props = defineProps<{
  team: Team
}>()

const emit = defineEmits<{
  (e: 'aliasesChanged'): void
}>()

const newAlias = ref("")
const errorMessage = ref("")
const busy = ref(false)

async function addAlias() {
  if (!newAlias.value.trim()) {
    return
  }
  busy.value = true
  errorMessage.value = ""
  try {
    await addTeamAlias(props.team.id, newAlias.value)
    newAlias.value = ""
    emit('aliasesChanged')
  } catch (error) {
    errorMessage.value = String(error)
  } finally {
    busy.value = false
  }
}

async function removeAlias(id: number) {
  busy.value = true
  errorMessage.value = ""
  try {
    await deleteTeamAlias(id)
    emit('aliasesChanged')
  } catch (error) {
    errorMessage.value = String(error)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="flex flex-col gap-2">
    <div class="flex flex-row flex-wrap items-center gap-1">
      <span class="text-sm opacity-70">Alternative spellings:</span>
      <span v-if="!(team.aliases?.length)" class="text-sm italic opacity-60">none</span>
      <span
          v-for="alias in team.aliases ?? []"
          :key="alias.id"
          class="bg-surface-500 rounded-xl px-2 py-1 text-sm flex flex-row items-center gap-1"
      >
        {{ alias.alias }}
        <i class="pi pi-times hover:cursor-pointer" @click="removeAlias(alias.id)"/>
      </span>
    </div>
    <div class="flex flex-row items-center gap-2">
      <InputText v-model="newAlias" placeholder="Add spelling" :disabled="busy" @keyup.enter="addAlias"/>
      <Button label="Add" :disabled="busy" @click="addAlias"/>
    </div>
    <div v-if="errorMessage" class="text-sm text-red-500">{{ errorMessage }}</div>
  </div>
</template>
```

- [ ] **Step 3: Wire it into the team list**

In `TeamList.vue`, import the editor and add it to the row body in both the amalgamation and the plain-team branch, below the existing name row. The plain-team branch (line 158) becomes:

```html
          <template v-else>
            <div class="flex flex-col">
              <div class="flex flex-row items-center">
                <div class="flex-1 align-middle inline-block">{{ data.name }}</div>
                <div>
                  <Button text label="History" @click="() => showHistory(data)"/>
                  <Button text label="Merge with..." @click="() => startMergeTeam(data)"/>
                  <Button text label="Convert" @click="() => startAmalgamationConvert(data)"/>
                </div>
              </div>
              <TeamAliasEditor :team="data" @aliases-changed="() => emit('teamUpdated', data)"/>
            </div>
          </template>
```

For the amalgamation branch, add `<TeamAliasEditor :team="data" @aliases-changed="() => emit('teamUpdated', data)"/>` as a new `col-span-2` div under the member chips (line 154).

The `teamUpdated` emit makes the parent reload teams, which is what refreshes the chips — aliases live on the team objects that come from the store.

- [ ] **Step 4: Verify**

```bash
cd frontend-vite && npm run type-check
```

Then in the running app, as an admin: add a spelling to a team, confirm the chip appears; add the same spelling to a different team and confirm the inline error names the owning team; remove a spelling and confirm the chip disappears. Then confirm in the report team picker that the new spelling finds the team.

- [ ] **Step 5: Commit**

```bash
git add frontend-vite/src/utils/api/teams_api.ts frontend-vite/src/types/team_types.ts \
        frontend-vite/src/components/admin/teams/TeamAliasEditor.vue \
        frontend-vite/src/components/admin/teams/TeamList.vue
git commit -m "feat: admin can manage alternative team spellings"
```

---

### Task 12: Merge dialog offers per-team aliases

**Files:**
- Modify: `frontend-vite/src/utils/api/teams_api.ts` (`mergeTeamsOnServer`)
- Modify: `frontend-vite/src/components/team/MergeTeamDialog.vue`

**Interfaces:**
- Consumes: `MergeTeamsDEO.aliasesToCreate` (Task 6, Task 9).
- Produces: `mergeTeamsOnServer(baseTeam, mergeTeams, changeDate?, aliasesToCreate?)`.

- [ ] **Step 1: Extend the API call**

```ts
export async function mergeTeamsOnServer(
    baseTeam: Team,
    mergeTeams: Array<Team>,
    changeDate?: string,
    aliasesToCreate?: Record<string, string>
): Promise<Team> {
    const data = MergeTeamsDEO.safeParse({
        baseTeam: baseTeam.id,
        teamsToMerge: mergeTeams.map(value => value.id),
        changeDate: changeDate,
        aliasesToCreate: aliasesToCreate ?? {}
    })
    if (!data.success) {
        return Promise.reject("Could not parse data")
    }
    return makePostRequest("/api/team/merge", data.data)
        .then(data => parseAndHandleDEO(data, Team))
}
```

- [ ] **Step 2: Add the per-team controls to the dialog**

In `MergeTeamDialog.vue`, add state that tracks a checkbox and editable text per team being merged, keyed by team id:

```ts
const aliasKeep = ref<Record<number, boolean>>({})
const aliasText = ref<Record<number, string>>({})

function trackAlias(team: Team) {
  aliasKeep.value[team.id] = true
  aliasText.value[team.id] = team.name
}

function untrackAlias(team: Team) {
  delete aliasKeep.value[team.id]
  delete aliasText.value[team.id]
}
```

Call `trackAlias` where teams are added and `untrackAlias` where they are removed, replacing the inline handlers on `TeamSelectField` (lines 86-87):

```html
              @team_selected="team => { teamsToMerge.push(team); trackAlias(team) }"
              @team_unselected="team => { teamsToMerge = teamsToMerge.filter(it => it.id !== team.id); untrackAlias(team) }"
```

Reset both maps in the existing `watch` on `props.visible` (line 35), alongside `teamsToMerge.value = []`:

```ts
    aliasKeep.value = {}
    aliasText.value = {}
```

Add the control block above the date picker (line 94):

```html
        <div v-if="teamsToMerge.length" class="flex flex-col gap-2 m-2">
          <div class="text-sm opacity-80">
            Keep the merged names as alternative spellings so referees can still find this team:
          </div>
          <div v-for="team in teamsToMerge" :key="team.id" class="flex flex-row items-center gap-2">
            <Checkbox v-model="aliasKeep[team.id]" :input-id="'keep_alias_' + team.id" binary/>
            <InputText v-model="aliasText[team.id]" :disabled="!aliasKeep[team.id]" class="flex-1"/>
          </div>
        </div>
```

And build the payload in `mergeTeams()`:

```ts
  const aliasesToCreate: Record<string, string> = {}
  for (const team of mergeFrom) {
    if (aliasKeep.value[team.id] && aliasText.value[team.id]?.trim()) {
      aliasesToCreate[String(team.id)] = aliasText.value[team.id].trim()
    }
  }
  mergeTeamsOnServer(mergeInto, mergeFrom, changeDateIso, aliasesToCreate)
```

- [ ] **Step 3: Verify**

```bash
cd frontend-vite && npm run type-check
```

In the running app: merge a team with the checkbox on and confirm the merged name appears as an alias of the survivor and finds it in the picker. Merge another with the checkbox off and confirm no alias was created. Merge a third with the text edited and confirm the edited text is what got stored.

- [ ] **Step 4: Commit**

```bash
git add frontend-vite/src/utils/api/teams_api.ts frontend-vite/src/components/team/MergeTeamDialog.vue
git commit -m "feat: merge dialog offers to keep merged names as spellings"
```

---

### Task 13: Rename dialogs offer to keep the old name

**Files:**
- Modify: `frontend-vite/src/utils/api/teams_api.ts` (`editTeamOnServer`)
- Modify: `frontend-vite/src/components/admin/teams/TeamList.vue`
- Modify: `frontend-vite/src/components/admin/teams/EditAmalgamationDialog.vue`
- Modify: `frontend-vite/src/components/admin/teams/ConvertTeamToAmalgamtionDialog.vue`

**Interfaces:**
- Consumes: `UpdateTeamDEO.keepOldNameAsAlias` (Task 7).
- Produces: `editTeamOnServer(team: Team, keepOldNameAsAlias?: string): Promise<Team>`.

Three components call `editTeamOnServer`. Only the paths where the name can actually change need the control, but all three must keep compiling.

- [ ] **Step 1: Extend the API call**

```ts
export async function editTeamOnServer(team: Team, keepOldNameAsAlias?: string): Promise<Team> {
    return makePostRequest("/api/team/update", {
        name: team.name,
        id: team.id,
        isAmalgamation: team.isAmalgamation,
        amalgamationTeams: team.amalgamationTeams ?? null,
        changeDate: team.changeDate,
        keepOldNameAsAlias: keepOldNameAsAlias
    })
        .then(data => parseAndHandleDEO(data, Team))
}
```

Building the body explicitly stops `aliases` from being posted back into a write payload that has no such field.

- [ ] **Step 2: Add the control to the team list row editor**

In `TeamList.vue`, `onRowEditInit` (line 43) already seeds edit state. Extend it to remember the original name and default the option on:

```ts
function onRowEditInit(event: DataTableRowEditInitEvent) {
  //Initialize the change date for the editor (defaults to today)
  event.data.changeDateValue = event.data.changeDate ? new Date(event.data.changeDate) : new Date()
  event.data.originalName = event.data.name
  event.data.keepOldName = true
  event.data.oldNameAlias = event.data.name
}
```

In the name column's `#editor` template (line 129), below the existing date picker:

```html
            <div v-if="slotProps.data.name !== slotProps.data.originalName" class="flex flex-col gap-1">
              <label class="flex flex-row items-center gap-2 text-xs">
                <Checkbox v-model="slotProps.data.keepOldName" binary/>
                Keep old name as alternative spelling
              </label>
              <InputText v-model="slotProps.data.oldNameAlias" :disabled="!slotProps.data.keepOldName"/>
            </div>
```

And in `editTeam` (line 48):

```ts
function editTeam(event: DataTableRowEditSaveEvent) {
  const {newData} = event
  const payload: Team = {
    name: newData.name,
    id: newData.id,
    isAmalgamation: newData.isAmalgamation,
    amalgamationTeams: newData.amalgamationTeams ?? null,
    changeDate: toIsoDate(newData.changeDateValue)
  }
  const nameChanged = newData.originalName !== undefined && newData.originalName !== newData.name
  const keepOldName = nameChanged && newData.keepOldName && newData.oldNameAlias?.trim()
      ? newData.oldNameAlias.trim()
      : undefined
  editTeamOnServer(payload, keepOldName)
      .then((dbTeam) => {
        emit('teamUpdated', dbTeam)
      })
      .catch((error) => {
        store.newError(error)
      })
}
```

- [ ] **Step 3: Handle the two dialogs**

Read `EditAmalgamationDialog.vue` and `ConvertTeamToAmalgamtionDialog.vue`. For each:

- If the component lets the user edit the team's name, add the same checkbox + editable text pair, defaulting to on and shown only once the name differs from the one the dialog opened with, and pass the value as the second argument to `editTeamOnServer`.
- If it does not touch the name (it only changes amalgamation membership or the amalgamation flag), leave it alone — the new parameter is optional, so the existing call still compiles and creates no alias.

- [ ] **Step 4: Verify**

```bash
cd frontend-vite && npm run type-check && npm run test:unit
```

In the running app: rename a team with the box ticked and confirm the old name still finds it in the picker; rename another with the box unticked and confirm the old name no longer finds it; edit a team's amalgamation membership without touching the name and confirm no alias is created.

- [ ] **Step 5: Commit**

```bash
git add frontend-vite/src/utils/api/teams_api.ts frontend-vite/src/components/admin/teams
git commit -m "feat: rename offers to keep the old team name as a spelling"
```

---

### Task 14: Full verification

**Files:** none — this task only runs and reports.

- [ ] **Step 1: Backend suite**

```bash
export JAVA_HOME=/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home
./gradlew test
```

Expected: BUILD SUCCESSFUL, every module green.

- [ ] **Step 2: Frontend suite and type-check**

```bash
cd frontend-vite && npm run test:unit && npm run type-check && npm run build
```

Expected: tests pass, no type errors, build succeeds.

- [ ] **Step 3: End-to-end walkthrough against a running app**

Work through this list in order and confirm each one:

1. An admin adds "Zurich" as a spelling of "Zürich Inneractive"; a referee searching `Zurich` sees the team with an "also known as" line.
2. Adding the same spelling to a second team is rejected with an error naming the first team.
3. Adding a spelling equal to another team's name is rejected.
4. Merging team B into team A with the alias box ticked makes B's name find A afterwards.
5. Merging with the box unticked creates no alias, and the merge still succeeds.
6. Renaming a team with the box ticked keeps the old name findable.
7. Team history shows "Alternative spelling added" and "Alternative spelling removed" entries.
8. A report rendered for a team with aliases still shows only the canonical name — check the report view, the public tournament report and the stats tables.

Item 8 is the one that matters most: the whole design rests on aliases never reaching a rendered name.

- [ ] **Step 4: Report**

State plainly which steps passed, and quote the output of anything that failed. Do not claim completion for anything not actually run.

---

## Self-Review

**Spec coverage:**

| Spec section | Task |
|---|---|
| Canonical display only | Constraint; verified in Task 14 step 3.8 |
| `TeamAliases` table, global unique `normalized` | Task 2 |
| Shared normalization, single implementation per runtime | Task 1, Task 9 |
| `ALIAS_ADDED` / `ALIAS_REMOVED` history | Task 2, Task 3 |
| Alias re-pointing when an aliased team is merged | Task 6 |
| `TeamDEO.aliases` | Task 4 |
| Admin-only alias routes | Task 5 |
| `MergeTeamsDEO.aliasesToCreate` | Task 6 |
| `UpdateTeamDEO.keepOldNameAsAlias` | Task 7 |
| Cache invalidation on every mutation | Task 3, Task 4 |
| Rejected alias never fails its operation | Task 6, Task 7 |
| Scoring table and sort order | Task 9 |
| Alias-matched subtitle | Task 10 |
| `:key` fix | Task 10 |
| Alias chips in `TeamList.vue` | Task 11 |
| Merge dialog per-team rows | Task 12 |
| Rename control in three components | Task 13 |
| Migration 10 + merged-team backfill | Task 2, Task 8 |
| No rename backfill from history | Task 8 |

**Deviations from the spec, both stated above:** `aliases` carries `TeamAliasDEO` objects rather than bare strings (the admin UI needs ids), and amalgamation member teams carry no aliases.

**Ordering:** Tasks 1-8 are backend and land in dependency order. Task 9 is the frontend foundation; 10-13 are the four UI surfaces and are independent of each other once 9 is done. Task 14 gates the branch.
