package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.Amalgamation
import eu.gaelicgames.referee.data.Amalgamations
import eu.gaelicgames.referee.data.GameReport
import eu.gaelicgames.referee.data.Team
import eu.gaelicgames.referee.data.TeamAlias
import eu.gaelicgames.referee.data.TeamAliases
import eu.gaelicgames.referee.data.TournamentReportTeamPreSelection
import eu.gaelicgames.referee.data.TournamentReportTeamPreSelections
import eu.gaelicgames.referee.data.api.ReportDEOTest
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class MergeTeamsDEOTest {
    companion object {
        private lateinit var tournamentReportData: TestHelper.TournamentReportData

        @JvmStatic
        @BeforeAll
        fun setUp(): Unit {
            TestHelper.setupDatabase()
            Companion.tournamentReportData = TestHelper.setUpReport()
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            TestHelper.tearDownDatabase()
        }

    }

    @Test
    fun `test merge teams with teams being preselected`() {
        runBlocking {
            var amalgamationId: Long = 0L
            var gameReportWithInjuryId = 0L
            var gameReportBaseTeamId = 0L
            var gameReportAmalgamationId = 0L
            var duplicateTeamId = 0L
            var baseTeamId = 0L
            var tournamentReportId = 0L
            newSuspendedTransaction {
                val baseTeam = Team.new {
                    name = "Base team"
                    isAmalgamation = false
                }
                baseTeamId = baseTeam.id.value

                val duplicateTeam = Team.new {
                    name = "Base team Duplicate"
                    isAmalgamation = false
                }
                duplicateTeamId = duplicateTeam.id.value
                commit()

                //Create an Amalgamation team from the duplicate
                val amalgamation = NewAmalgamationDEO(
                    "Duplicate Amalgamation Squad",
                    listOf(TeamDEO.fromTeam(duplicateTeam))
                ).createInDatabase()
                amalgamationId = amalgamation.getOrThrow().id.value
                commit()
                //Create a new tournament report with the created teams
                val tournamentReport = TestHelper.setUpReport(
                    tournamentReportData.tournamentReport.tournament,
                    preselectedTeams = listOf(
                        baseTeam,
                        duplicateTeam,
                        amalgamation.getOrThrow()
                    ),
                    makeLeagueRound = false
                )
                tournamentReportId = tournamentReport.tournamentReportID

                //Setup Tournament Team Preselections
                val preselection = TournamentTeamPreselectionDEO(
                    tournamentReportData.tournamentReport.id.value,
                    listOf(
                        tournamentReportData.teamIDs[0],
                        duplicateTeam.id.value,
                        amalgamation.getOrThrow().id.value
                    )
                ).add()


                //Make full use of the duplicate team
                var (gameReportWithInjury, _, _) = TestHelper.initializeGameReportAndDisciplinaryAction(
                    TestHelper.TournamentReportData(
                        tournamentReportData.tournamentReport,
                        tournamentReportData.tournamentReportID,
                        mutableListOf(
                            duplicateTeam.id.value,
                            tournamentReportData.teamIDs[0],
                        ),
                        tournamentReportData.gameTypeIDs,
                        tournamentReportData.extraTimeIDs,
                        tournamentReportData.gameLengthIDs
                    )
                )
                TestHelper.initializeInjury(gameReportWithInjury)
                gameReportWithInjuryId = gameReportWithInjury.id.value

                var (gameReportBaseTeam, _, _) = TestHelper.initializeGameReportAndDisciplinaryAction(
                    TestHelper.TournamentReportData(
                        tournamentReportData.tournamentReport,
                        tournamentReportData.tournamentReportID,
                        mutableListOf(
                            baseTeam.id.value,
                            tournamentReportData.teamIDs[0],
                        ),
                        tournamentReportData.gameTypeIDs,
                        tournamentReportData.extraTimeIDs,
                        tournamentReportData.gameLengthIDs
                    )
                )
                gameReportBaseTeamId = gameReportBaseTeam.id.value

                var (gameReportAmalgamation, _, _) = TestHelper.initializeGameReportAndDisciplinaryAction(
                    TestHelper.TournamentReportData(
                        tournamentReportData.tournamentReport,
                        tournamentReportData.tournamentReportID,
                        mutableListOf(
                            amalgamation.getOrThrow().id.value,
                            tournamentReportData.teamIDs[0],
                        ),
                        tournamentReportData.gameTypeIDs,
                        tournamentReportData.extraTimeIDs,
                        tournamentReportData.gameLengthIDs
                    )
                )
                gameReportAmalgamationId = gameReportAmalgamation.id.value

                commit()
                val result = MergeTeamsDEO(
                    baseTeam.id.value,
                    listOf(
                        duplicateTeamId
                    )
                ).updateInDatabase()

                commit()
                println(result)

                assert(result.isSuccess) {
                    "Merge teams should be successful"
                }
            }
            newSuspendedTransaction {
                //First check if the duplicate team is soft-deleted in the db
                val newDuplicateTeamQueried = Team.findById(duplicateTeamId)
                assert(newDuplicateTeamQueried != null) {
                    "Duplicate team should still exist (soft-deleted), but was not found"
                }
                assert(newDuplicateTeamQueried!!.deletedAt != null) {
                    "Duplicate team should be soft-deleted (deleted_at set)"
                }
                assert(newDuplicateTeamQueried.mergedInto?.id?.value == baseTeamId) {
                    "Duplicate team should have been merged into the base team"
                }


                //Check if amalgamation team is still there and updated
                assert(Team.findById(amalgamationId) != null) {
                    "Amalgamation team should be there"
                }


                assert(Amalgamation.find {
                    Amalgamations.addedTeam eq duplicateTeamId
                }.count() == 0L) {
                    "Amalgamation should not contain the duplicate team anymore"
                }

                assert(Amalgamation.find {
                    Amalgamations.addedTeam eq baseTeamId
                }.count() == 1L) {
                    "Amalgamation should contain the base team, but only once"
                }

                //Check if the TournamentTeamPreselection does not contain references to the duplicate team anymore
                val tournamentTeamPreselection = TournamentReportTeamPreSelection.find {
                    TournamentReportTeamPreSelections.team eq duplicateTeamId
                }
                assert(tournamentTeamPreselection.count() == 0L) {
                    "TournamentTeamPreselection should not contain the duplicate team anymore"
                }

                //Check if the TournamentTeamPreselection contains references to the base team
                val tournamentTeamPreselectionBaseTeam = TournamentReportTeamPreSelection.find {
                    TournamentReportTeamPreSelections.team eq baseTeamId
                }
                assert(tournamentTeamPreselectionBaseTeam.count() == 1L) {
                    "TournamentTeamPreselection should contain the base team, but only once"
                }

                //Then check the game reports
                val gameReportWithInjury = GameReport.findById(gameReportWithInjuryId)
                assert(gameReportWithInjury?.teamA?.id?.value == baseTeamId) {
                    "Game report with injury should have base team as team A"
                }

                val gameReportBaseTeam = GameReport.findById(gameReportBaseTeamId)
                assert(gameReportBaseTeam?.teamA?.id?.value == baseTeamId) {
                    "Game report with base team should have base team as team A"
                }

                val gameReportAmalgamation = GameReport.findById(gameReportAmalgamationId)
                assert(gameReportAmalgamation?.teamA?.id?.value == amalgamationId) {
                    "Game report with amalgamation should have amalgamation team as team A"
                }

                //Check if TournamentReportTeamPreSelections are updated
                val tournamentReportTeamPreSelection = TournamentReportTeamPreSelection.find {
                    TournamentReportTeamPreSelections.report eq tournamentReportId
                }
                assert(tournamentReportTeamPreSelection.count() == 2L) {
                    "TournamentReportTeamPreSelections should contain two teams, but found ${tournamentReportTeamPreSelection.count()}"
                }

            }

        }
    }

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

    @Test
    fun `carried alias wins when the requested alias has the same spelling`() {
        runBlocking {
            var baseId = 0L
            var mergedId = 0L
            newSuspendedTransaction {
                baseId = Team.new { name = "SameSpelling Base"; isAmalgamation = false }.id.value
                mergedId = Team.new { name = "SameSpelling Merged"; isAmalgamation = false }.id.value
                commit()
            }
            val carriedAliasId = NewTeamAliasDEO(mergedId, "SameSpelling Shared")
                .createInDatabase().getOrThrow().id

            MergeTeamsDEO(
                baseTeam = baseId,
                teamsToMerge = listOf(mergedId),
                aliasesToCreate = mapOf(mergedId to "SameSpelling Shared")
            ).updateInDatabase().getOrThrow()

            newSuspendedTransaction {
                val aliases = TeamAlias.find { TeamAliases.team eq baseId }.toList()
                val matching = aliases.filter { it.alias == "SameSpelling Shared" }
                assertEquals(1, matching.size, "there should be exactly one alias with that spelling")
                assertEquals(
                    carriedAliasId, matching.first().id.value,
                    "the carried alias row must be the one that survives, not a freshly created one"
                )
            }
        }
    }

    // Note: two live aliases can never share a spelling (TeamAliases.normalized has a real DB-level
    // unique index, enforced independently of validateAliasInTransaction), so "two merged teams each
    // already carrying an identical-spelling alias" cannot be constructed even by inserting rows
    // directly - Postgres itself would reject the second insert. The nearest constructible equivalent
    // that still exercises the carried-alias collision -> delete (not carry, not fail) branch is a
    // carried alias whose spelling collides with another *team's own live canonical name* - which is
    // reachable because team creation never cross-checks the alias table.
    @Test
    fun `carried alias colliding with another merging team's live name is dropped, not carried, merge still succeeds`() {
        runBlocking {
            var baseId = 0L
            var mergedAId = 0L
            var mergedBId = 0L
            newSuspendedTransaction {
                baseId = Team.new { name = "CarriedVsLiveName Base"; isAmalgamation = false }.id.value
                mergedAId = Team.new { name = "CarriedVsLiveName Merged A"; isAmalgamation = false }.id.value
                commit()
            }
            // Create the alias while no team is yet named "CarriedVsLiveName Merged B", so creation
            // passes validation; only create team B (with that exact name) afterwards, so the
            // collision only exists once the merge re-validates the carried alias.
            NewTeamAliasDEO(mergedAId, "CarriedVsLiveName Merged B").createInDatabase().getOrThrow()
            newSuspendedTransaction {
                mergedBId = Team.new { name = "CarriedVsLiveName Merged B"; isAmalgamation = false }.id.value
                commit()
            }

            val result = MergeTeamsDEO(
                baseTeam = baseId,
                teamsToMerge = listOf(mergedAId, mergedBId),
                aliasesToCreate = emptyMap()
            ).updateInDatabase()

            assertTrue(result.isSuccess, "merge must succeed even though the carried alias collides with team B's live name")
            newSuspendedTransaction {
                val matching = TeamAlias.find { TeamAliases.team eq baseId }
                    .filter { it.alias == "CarriedVsLiveName Merged B" }
                assertTrue(matching.isEmpty(), "the colliding carried alias should have been deleted, not carried")
                assertNotNull(Team.findById(mergedAId)!!.mergedInto)
                assertNotNull(Team.findById(mergedBId)!!.mergedInto)
            }
        }
    }

}
