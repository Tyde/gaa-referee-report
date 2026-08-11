package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.Amalgamation
import eu.gaelicgames.referee.data.Amalgamations
import eu.gaelicgames.referee.data.Team
import eu.gaelicgames.referee.data.TeamChangeType
import eu.gaelicgames.referee.data.TeamHistoryEvent
import eu.gaelicgames.referee.data.TeamHistoryEvents
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TeamHistoryDEOTest {

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

    private fun historyEventsOf(teamId: Long): List<TeamHistoryEventDEO> {
        return runBlocking {
            TeamDEO.historyForTeam(teamId)
        }
    }

    @Test
    fun `team creation writes a CREATED history event`() {
        runBlocking {
            val teamId = newSuspendedTransaction {
                val team = Team.new {
                    name = "History Test Team"
                    isAmalgamation = false
                }
                writeTeamHistoryEventInTransaction(
                    team, TeamChangeType.CREATED, LocalDate.now(),
                    null, team.name, null
                )
                team.id.value
            }
            val events = historyEventsOf(teamId)
            assert(events.size == 1) { "Expected exactly one event, found ${events.size}" }
            assert(events[0].changeType == TeamChangeType.CREATED.name) { "Expected CREATED, found ${events[0].changeType}" }
            assert(events[0].newValue == "History Test Team") { "Expected name as newValue, found ${events[0].newValue}" }
        }
    }

    @Test
    fun `renaming a team writes a RENAMED event with old and new name and date`() {
        runBlocking {
            val teamId = newSuspendedTransaction {
                val team = Team.new {
                    name = "Old Name"
                    isAmalgamation = false
                }
                team.id.value
            }
            val changeDate = LocalDate.of(2024, 3, 15)
            val result = TeamDEO(
                name = "New Name",
                id = teamId,
                isAmalgamation = false,
                amalgamationTeams = null,
                changeDate = changeDate
            ).updateInDatabase()
            assert(result.isSuccess) { "Update should be successful" }

            val events = historyEventsOf(teamId)
            val renamed = events.firstOrNull { it.changeType == TeamChangeType.RENAMED.name }
            assert(renamed != null) { "Expected a RENAMED event, found ${events.map { it.changeType }}" }
            assert(renamed!!.oldValue == "Old Name") { "Expected old name 'Old Name', found ${renamed.oldValue}" }
            assert(renamed.newValue == "New Name") { "Expected new name 'New Name', found ${renamed.newValue}" }
            assert(renamed.changeDate == changeDate) { "Expected changeDate $changeDate, found ${renamed.changeDate}" }
        }
    }

    @Test
    fun `converting a team to an amalgamation writes CONVERTED and MEMBER events`() {
        runBlocking {
            val (teamId, memberId) = newSuspendedTransaction {
                val team = Team.new {
                    name = "Squad Team"
                    isAmalgamation = false
                }
                val member = Team.new {
                    name = "Member Team"
                    isAmalgamation = false
                }
                Pair(team.id.value, member.id.value)
            }
            val result = TeamDEO(
                name = "Squad Team",
                id = teamId,
                isAmalgamation = true,
                amalgamationTeams = listOf(
                    TeamDEO("Member Team", memberId, false, null)
                )
            ).updateInDatabase()
            assert(result.isSuccess) { "Conversion should be successful" }

            val events = historyEventsOf(teamId)
            val types = events.map { it.changeType }
            assert(TeamChangeType.CONVERTED_TO_AMALGAMATION.name in types) {
                "Expected CONVERTED_TO_AMALGAMATION event, found $types"
            }
            assert(TeamChangeType.MEMBER_ADDED.name in types) {
                "Expected MEMBER_ADDED event, found $types"
            }
            val memberAdded = events.first { it.changeType == TeamChangeType.MEMBER_ADDED.name }
            assert(memberAdded.newValue == "Member Team") { "Expected member name in newValue, found ${memberAdded.newValue}" }
        }
    }

    @Test
    fun `removing a member from an amalgamation writes a MEMBER_REMOVED event`() {
        runBlocking {
            val teamId = newSuspendedTransaction {
                val team = Team.new {
                    name = "Amalgamation Squad"
                    isAmalgamation = true
                }
                val member = Team.new {
                    name = "Member To Remove"
                    isAmalgamation = false
                }
                Amalgamation.new {
                    amalgamation = team
                    addedTeam = member
                }
                team.id.value
            }

            // Remove the member
            val result = TeamDEO(
                name = "Amalgamation Squad",
                id = teamId,
                isAmalgamation = true,
                amalgamationTeams = listOf()
            ).updateInDatabase()
            assert(result.isSuccess) { "Update should be successful" }

            val events = historyEventsOf(teamId)
            val removed = events.firstOrNull { it.changeType == TeamChangeType.MEMBER_REMOVED.name }
            assert(removed != null) { "Expected a MEMBER_REMOVED event, found ${events.map { it.changeType }}" }
            assert(removed!!.oldValue == "Member To Remove") { "Expected removed member name in oldValue, found ${removed.oldValue}" }
        }
    }

    @Test
    fun `merging teams soft-deletes the merged team and writes history events`() {
        runBlocking {
            val (baseId, mergeId) = newSuspendedTransaction {
                val base = Team.new {
                    name = "Base Team"
                    isAmalgamation = false
                }
                val merge = Team.new {
                    name = "Merged Team"
                    isAmalgamation = false
                }
                Pair(base.id.value, merge.id.value)
            }

            val result = MergeTeamsDEO(baseId, listOf(mergeId)).updateInDatabase()
            assert(result.isSuccess) { "Merge should be successful" }

            newSuspendedTransaction {
                val mergedTeam = Team.findById(mergeId)
                assert(mergedTeam != null) { "Merged team should still exist (soft-deleted)" }
                assert(mergedTeam!!.deletedAt != null) { "Merged team should be soft-deleted" }
                assert(mergedTeam.mergedInto?.id?.value == baseId) { "Merged team should point to base team" }
            }

            // Merged team has a MERGED_INTO event
            val mergedEvents = historyEventsOf(mergeId)
            val mergedInto = mergedEvents.firstOrNull { it.changeType == TeamChangeType.MERGED_INTO.name }
            assert(mergedInto != null) { "Expected MERGED_INTO event on merged team, found ${mergedEvents.map { it.changeType }}" }
            assert(mergedInto!!.oldValue == "Merged Team") { "Expected old name in oldValue, found ${mergedInto.oldValue}" }
            assert(mergedInto.newValue == "Base Team") { "Expected base name in newValue, found ${mergedInto.newValue}" }
        }
    }

    @Test
    fun `public team list excludes soft-deleted teams`() {
        runBlocking {
            val (baseId, mergeId) = newSuspendedTransaction {
                val base = Team.new {
                    name = "List Base Team"
                    isAmalgamation = false
                }
                val merge = Team.new {
                    name = "List Merged Team"
                    isAmalgamation = false
                }
                Pair(base.id.value, merge.id.value)
            }
            MergeTeamsDEO(baseId, listOf(mergeId)).updateInDatabase()

            val allTeams = TeamDEO.allTeamList()
            assert(allTeams.none { it.id == mergeId }) {
                "Soft-deleted team should not appear in the public team list"
            }
            assert(allTeams.any { it.id == baseId }) {
                "Base team should still appear in the public team list"
            }
        }
    }

    @Test
    fun `history events are sorted by change date`() {
        runBlocking {
            val teamId = newSuspendedTransaction {
                val team = Team.new {
                    name = "Sorting Team"
                    isAmalgamation = false
                }
                team.id.value
            }
            //Backdate a rename to before the creation date
            val past = LocalDate.of(2020, 1, 1)
            TeamDEO(
                name = "Sorting Team Renamed",
                id = teamId,
                isAmalgamation = false,
                amalgamationTeams = null,
                changeDate = past
            ).updateInDatabase()

            val events = historyEventsOf(teamId)
            val dates = events.map { it.changeDate }
            assert(dates == dates.sorted()) { "History events should be sorted by change date, found $dates" }
        }
    }

    @Test
    fun `merging a team that is member of an amalgamation avoids duplicate members`() {
        runBlocking {
            val (baseId, mergeId, amalgamationId) = newSuspendedTransaction {
                val base = Team.new {
                    name = "Amalgamation Base"
                    isAmalgamation = false
                }
                val merge = Team.new {
                    name = "Amalgamation Merge"
                    isAmalgamation = false
                }
                val squad = Team.new {
                    name = "Shared Squad"
                    isAmalgamation = true
                }
                Amalgamation.new {
                    amalgamation = squad
                    addedTeam = base
                }
                Amalgamation.new {
                    amalgamation = squad
                    addedTeam = merge
                }
                Triple(base.id.value, merge.id.value, squad.id.value)
            }

            val result = MergeTeamsDEO(baseId, listOf(mergeId)).updateInDatabase()
            assert(result.isSuccess) { "Merge should be successful" }

            newSuspendedTransaction {
                val members = Amalgamation.find { Amalgamations.amalgamation eq amalgamationId }.toList()
                assert(members.size == 1) { "Amalgamation should have exactly one member, found ${members.size}" }
                assert(members[0].addedTeam.id.value == baseId) {
                    "Amalgamation member should be the base team, found ${members[0].addedTeam.name}"
                }
            }

            val events = historyEventsOf(amalgamationId)
            val removed = events.firstOrNull { it.changeType == TeamChangeType.MEMBER_REMOVED.name }
            assert(removed != null) { "Expected MEMBER_REMOVED event on amalgamation, found ${events.map { it.changeType }}" }
            assert(removed!!.oldValue == "Amalgamation Merge") { "Expected merged team name in oldValue, found ${removed.oldValue}" }
        }
    }
}
