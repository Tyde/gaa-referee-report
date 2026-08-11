package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.Team
import eu.gaelicgames.referee.data.TeamAlias
import eu.gaelicgames.referee.data.TeamAliases
import eu.gaelicgames.referee.data.TeamChangeType
import eu.gaelicgames.referee.data.TeamHistoryEvent
import eu.gaelicgames.referee.data.TeamHistoryEvents
import eu.gaelicgames.referee.util.lockedTransaction
import eu.gaelicgames.referee.util.normalizeTeamName
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun `create alias succeeds and records history`() {
        runBlocking {
            var teamId = 0L
            newSuspendedTransaction {
                teamId = Team.new {
                    name = "Create Alias Origin FC"
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

    // ---------------------------------------------------------------------
    // Regression coverage for the "fromTeam outside a transaction" bug:
    // the production route handlers call `fromTeam` (directly or via
    // `.map { ... }`) AFTER the transaction that created/updated the team
    // has already closed. Unlike the tests above, these deliberately do NOT
    // wrap the call under test in a `newSuspendedTransaction` of their own -
    // they call the exact production sequence (DB mutation, then a fresh
    // `lockedTransaction { TeamDEO.fromTeam(...) }`, matching the fixed
    // routing code) at the top level, so a missing transaction wrap around
    // `fromTeam` at any of those call sites would surface as a thrown
    // exception here, just like it did at the HTTP layer.
    // ---------------------------------------------------------------------

    @Test
    fun `fromTeam works right after plain entity creation, mirroring the NewTeam route`() {
        runBlocking {
            // Mirrors RefereeApiRouting's `Api.NewTeam` handler: the team is
            // created inside its own `lockedTransaction`, which has already
            // committed and closed by the time `fromTeam` is called.
            val newTeamDB = lockedTransaction {
                Team.new { name = "Regression New Team FC"; isAmalgamation = false }
            }

            val deo = lockedTransaction { TeamDEO.fromTeam(newTeamDB) }

            assertEquals("Regression New Team FC", deo.name)
            assertEquals(emptyList<String>(), deo.aliases!!.map { it.alias })
        }
    }

    @Test
    fun `fromTeam works after createInDatabase resolves, mirroring the NewAmalgamation route`() {
        runBlocking {
            var memberTeamId = 0L
            newSuspendedTransaction {
                memberTeamId = Team.new { name = "Regression Member FC"; isAmalgamation = false }.id.value
                commit()
            }
            NewTeamAliasDEO(memberTeamId, "Regression Member Spelling").createInDatabase().getOrThrow()
            val memberDeo = TeamDEO(
                name = "Regression Member FC", id = memberTeamId, isAmalgamation = false, amalgamationTeams = null
            )

            // Mirrors RefereeApiRouting's `Api.NewAmalgamation` handler exactly:
            // `createInDatabase()` opens and closes its own transaction internally,
            // and its `Result` is mapped to `fromTeam` outside of that transaction.
            val result = NewAmalgamationDEO("Regression Amalgamation FC", listOf(memberDeo)).createInDatabase(null)
                .map { lockedTransaction { TeamDEO.fromTeam(it) } }

            assertTrue(result.isSuccess, "expected success, got ${result.exceptionOrNull()}")
            assertEquals("Regression Amalgamation FC", result.getOrThrow().name)
            assertEquals(emptyList<String>(), result.getOrThrow().aliases!!.map { it.alias })
        }
    }

    @Test
    fun `fromTeam carries aliases after updateInDatabase resolves, mirroring the Team Update route`() {
        runBlocking {
            var teamId = 0L
            newSuspendedTransaction {
                teamId = Team.new { name = "Regression Update FC"; isAmalgamation = false }.id.value
                commit()
            }
            NewTeamAliasDEO(teamId, "Regression Update Spelling").createInDatabase().getOrThrow()
            val toUpdate = TeamDEO(
                name = "Regression Update FC Renamed", id = teamId, isAmalgamation = false, amalgamationTeams = null
            )

            // Mirrors AdminApiRouting's `Api.Team.Update` handler exactly:
            // `updateInDatabase()` opens and closes its own transaction internally,
            // and its `Result` is mapped to `fromTeam` outside of that transaction.
            val result = toUpdate.updateInDatabase(null).map { lockedTransaction { TeamDEO.fromTeam(it) } }

            assertTrue(result.isSuccess, "expected success, got ${result.exceptionOrNull()}")
            val deo = result.getOrThrow()
            assertEquals("Regression Update FC Renamed", deo.name)
            assertEquals(listOf("Regression Update Spelling"), deo.aliases!!.map { it.alias })
        }
    }
}
