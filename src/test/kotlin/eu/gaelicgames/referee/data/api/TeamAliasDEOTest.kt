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
