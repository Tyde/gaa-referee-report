package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.Team
import eu.gaelicgames.referee.data.TeamAlias
import eu.gaelicgames.referee.data.TeamAliases
import eu.gaelicgames.referee.data.TeamChangeType
import eu.gaelicgames.referee.data.TeamHistoryEvent
import eu.gaelicgames.referee.data.TeamHistoryEvents
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
}
