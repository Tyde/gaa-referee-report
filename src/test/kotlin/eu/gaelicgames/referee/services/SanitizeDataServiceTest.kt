package eu.gaelicgames.referee.services

import eu.gaelicgames.referee.data.GameCode
import eu.gaelicgames.referee.data.Team
import eu.gaelicgames.referee.data.TournamentReport
import eu.gaelicgames.referee.data.TournamentReports
import eu.gaelicgames.referee.data.api.TestHelper
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SanitizeDataServiceTest {
    companion object {
        private lateinit var tournamentReportData: TestHelper.TournamentReportData





    }

    @BeforeEach
    fun setUp(): Unit {
        TestHelper.setupDatabase()
        tournamentReportData = TestHelper.setUpReport()
    }

    @AfterEach
    fun tearDownEach() {
        println("Run after Each")
        TestHelper.tearDownDatabase()
    }

    @Test
    fun `test mergeTournamentReports`() {
        runBlocking {
            val creationData = initializeData()
            val sanitizeDataService = SanitizeDataService(this)

            sanitizeDataService.runCycle()

            transaction {
                //There should now be only two reports left
                TournamentReport.all().map {
                    "TournamentReport: ${it.id.value} ${it.tournament.name} ${it.referee.id.value} ${it.selectedTeams.map { it.id.value }} ${it.gameReports.map { it.id.value }}"
                 }.forEach { println(it) }
                assert(TournamentReports.selectAll().count() == 5L)
                //We find the merged reports by referee id
                val mergedReport =
                    TournamentReport.find { TournamentReports.referee.eq(creationData.first)}.firstOrNull()

                //We check if the merged reports have the same game reports
                assert(mergedReport?.gameReports?.map { it.id.value }?.toSet() == creationData.second.toSet())

                //We check if the merged reports have the same teams
                assert(mergedReport?.selectedTeams?.map { it.id.value }?.toSet() == creationData.third.toSet())
            }
        }
    }

    @Test
    fun `test mergeTournamentReports skip league rounds`() {
        runBlocking{

            val creationData = initializeData(leagueRound = true)

            val sanitizeDataService = SanitizeDataService(this)
            transaction{
                //All reports should still exist and should not be merged
                TournamentReport.all().map {
                    "TournamentReport: ${it.id.value} ${it.tournament.name} (${it.tournament.id}, league: ${it.tournament.isLeague}) Refereee: ${it.referee.id.value} Teams: ${it.selectedTeams.map { it.id.value }} Game Reports: ${it.gameReports.map { it.id.value }}"
                }.forEach { println(it) }


            }
            println("Run cycle")
            sanitizeDataService.runCycle()

            transaction{
                //All reports should still exist and should not be merged
                TournamentReport.all().map {
                    "TournamentReport: ${it.id.value} ${it.tournament.name} (${it.tournament.id}, league: ${it.tournament.isLeague}) Refereee: ${it.referee.id.value} Teams: ${it.selectedTeams.map { it.id.value }} Game Reports: ${it.gameReports.map { it.id.value }}"
                }.forEach { println(it) }
                println(TournamentReports.selectAll().count())
                assert(TournamentReports.selectAll().count() == 8L)

            }

        }

    }

    private suspend fun initializeData(leagueRound: Boolean = false) = newSuspendedTransaction {

        val firstReport = if(leagueRound) {
            TestHelper.setUpReport(makeLeagueRound = true)
        } else {
            tournamentReportData
        }
        firstReport.tournamentReport.isSubmitted = true
        commit()

        TestHelper.initializeGameReport(firstReport)
        TestHelper.initializeGameReport(firstReport)
        TestHelper.initializeGameReport(firstReport)


        val teamsSecond = listOf(
            Team.new {
                name = "Second Team"
                isAmalgamation = false
            }, Team.new {
                name = "Third Team"
                isAmalgamation = false
            }
        ) + firstReport.tournamentReport.selectedTeams.toList()

        val secondReport = TestHelper.setUpReport(
            firstReport.tournamentReport.tournament,
            firstReport.tournamentReport.referee,
            teamsSecond
        )
        secondReport.tournamentReport.isSubmitted = true
        commit()
        var gr = TestHelper.initializeGameReport(secondReport)
        if (leagueRound) {
            gr?.startTime = gr?.startTime?.minusDays(1)
        }
        gr = TestHelper.initializeGameReport(secondReport)
        if (leagueRound) {
            gr?.startTime = gr?.startTime?.minusDays(1)
        }
        gr = TestHelper.initializeGameReport(secondReport)
        if (leagueRound) {
            gr?.startTime = gr?.startTime?.minusDays(1)
        }

        val thirdReport = TestHelper.setUpReport(
            firstReport.tournamentReport.tournament,
            firstReport.tournamentReport.referee
        )
        thirdReport.tournamentReport.isSubmitted = true
        commit()
        gr = TestHelper.initializeGameReport(thirdReport)

        gr = TestHelper.initializeGameReport(thirdReport)

        gr = TestHelper.initializeGameReport(thirdReport)

        val mergedGRIds = firstReport.tournamentReport.gameReports.map { it.id.value } +
                secondReport.tournamentReport.gameReports.map { it.id.value } +
                thirdReport.tournamentReport.gameReports.map { it.id.value }

        val teamIDs = (firstReport.teamIDs + secondReport.teamIDs + thirdReport.teamIDs).toSet().toList()

        val unaffectedReport = TestHelper.setUpReport()
        unaffectedReport.tournamentReport.isSubmitted = true
        commit()
        TestHelper.initializeGameReport(unaffectedReport)
        TestHelper.initializeGameReport(unaffectedReport)
        TestHelper.initializeGameReport(unaffectedReport)

        val unaffectedReportSameReferee = TestHelper.setUpReport(
            null,
            thirdReport.tournamentReport.referee
        )
        unaffectedReportSameReferee.tournamentReport.isSubmitted = true
        commit()
        TestHelper.initializeGameReport(unaffectedReportSameReferee)

        val unaffectedReportSameTournament = TestHelper.setUpReport(
            firstReport.tournamentReport.tournament,
            null,
        )
        unaffectedReportSameTournament.tournamentReport.isSubmitted = true
        commit()
        TestHelper.initializeGameReport(unaffectedReportSameTournament)

        val unaffectedReportSameRefereeAndTournamentWrongCode = TestHelper.setUpReport(
            firstReport.tournamentReport.tournament,
            firstReport.tournamentReport.referee
        )
        unaffectedReportSameRefereeAndTournamentWrongCode.tournamentReport.code = GameCode.all().toList()[1]
        unaffectedReportSameRefereeAndTournamentWrongCode.tournamentReport.isSubmitted = true
        commit()
        TestHelper.initializeGameReport(unaffectedReportSameRefereeAndTournamentWrongCode)
        return@newSuspendedTransaction Triple(
            firstReport.tournamentReport.referee.id.value,
            mergedGRIds,
            teamIDs
        )
    }



    @Test
    fun `test fixDatesOnOneDayTournaments`() {
        runBlocking {
            newSuspendedTransaction {

            }
        }
    }
}
