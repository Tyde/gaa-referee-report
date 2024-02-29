package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TournamentDEOTest {
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
    fun tournamentDEO_store() {
        runBlocking {
            val region = transaction { Region.all().first().id.value }
            val tournament = NewTournamentDEO(
                "Test Tournament",
                "Test Location",
                LocalDate.now(),
                region
            )
            val result = tournament.storeInDatabase()
            assert(result.isSuccess)
            val tournamentRes = result.getOrNull()
            assert(tournamentRes != null)
            transaction {
                val tournamentDB = Tournament.findById(tournamentRes!!.id)
                assert(tournamentDB != null)
                assert(tournamentDB!!.name == tournament.name)
                assert(tournamentDB.location == tournament.location)
                assert(tournamentDB.date == tournament.date)
                assert(tournamentDB.region.id.value == tournament.region)
            }
        }
    }

    @Test
    fun tournamentDEO_store_invalidRegion() {
        runBlocking {
            val firstInvalid = transaction { Region.all().map { it.id.value }.max().plus(1)?:0}
            val tournament = NewTournamentDEO(
                "Test Tournament",
                "Test Location",
                LocalDate.now(),
                firstInvalid
            )
            val result = tournament.storeInDatabase()
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun tournamentDEO_store_blankName() {
        runBlocking {
            val region = transaction { Region.all().first().id.value }
            val tournament = NewTournamentDEO(
                "",
                "Test Location",
                LocalDate.now(),
                region
            )
            val result = tournament.storeInDatabase()
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun tournamentDEO_update() {
        runBlocking {
            val tournament = transaction { Tournament.all().first() }
            val newRegion = transaction { Region.all().last().id.value }
            val newTournament = TournamentDEO(
                tournament.id.value,
                "New Name",
                "New Location",
                LocalDate.now(),
                newRegion
            )
            val result = newTournament.updateInDatabase()
            assert(result.isSuccess)
            val tournamentRes = result.getOrNull()
            assert(tournamentRes != null)
            transaction {
                val tournamentDB = Tournament.findById(tournamentRes!!.id)
                assert(tournamentDB != null)
                assert(tournamentDB!!.name == newTournament.name)
                assert(tournamentDB.location == newTournament.location)
                assert(tournamentDB.date == newTournament.date)
                assert(tournamentDB.region.id.value == newTournament.region)
            }
        }
    }

    @Test
    fun tournamentDEO_update_invalidRegion() {
        runBlocking {
            val tournament = transaction { Tournament.all().first() }
            val firstInvalid = transaction { Region.all().map { it.id.value }.max().plus(1)?:0}
            val newTournament = TournamentDEO(
                tournament.id.value,
                "New Name",
                "New Location",
                LocalDate.now(),
                firstInvalid
            )
            val result = newTournament.updateInDatabase()
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun tournamentDEO_update_invalidTournament() {
        runBlocking {
            val firstInvalid = transaction { Tournament.all().map { it.id.value }.max().plus(1)?:0}
            val newTournament = TournamentDEO(
                firstInvalid,
                "New Name",
                "New Location",
                LocalDate.now(),
                1
            )
            val result = newTournament.updateInDatabase()
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun tournamentDEO_update_blankName() {
        runBlocking {
            val tournament = transaction { Tournament.all().first() }
            val newRegion = transaction { Region.all().last().id.value }
            val newTournament = TournamentDEO(
                tournament.id.value,
                "",
                "New Location",
                LocalDate.now(),
                newRegion
            )
            val result = newTournament.updateInDatabase()
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun publicTournamentDEO_fromTournament() {
        runBlocking {
            val tournamentID = generateFakeCompleteTournament()
            val publicReportDEO = PublicTournamentReportDEO.fromTournamentId(tournamentID)
            assert(publicReportDEO.tournament.id == tournamentID)
            transaction {
                val numGameReports = TournamentReport.find { TournamentReports.tournament eq tournamentID }
                    .filter { it.isSubmitted }
                    .map { it.gameReports.count() }
                    .sum()
                assert(publicReportDEO.games.size.toLong() == numGameReports)
            }

        }
    }

    private suspend fun generateFakeCompleteTournament(): Long {
        val report = TestHelper.setUpReport()
        val generatedData = newSuspendedTransaction {
            (1..3).map {
                val code = GameCode.all().toList().get(it)
                val tr = TournamentReport.new {
                    this.tournament = report.tournamentReport.tournament
                    this.referee = report.tournamentReport.referee
                    this.code = code
                    additionalInformation = ""
                    isSubmitted = true
                }
                commit()
                val trd = TestHelper.TournamentReportData(
                    tr,
                    tr.id.value,
                    report.teamIDs,
                    report.gameTypeIDs,
                    report.extraTimeIDs
                )
                val gr = TestHelper.initializeGameReportAndDisciplinaryAction(trd)
                Pair(trd, gr)
            }
        }
        val tournamentID = transaction { report.tournamentReport.tournament.id.value }
        return tournamentID
    }

    @Test
    fun completeTournamentDEO_fromTournamentID() {
        runBlocking {
            val tournamentID = generateFakeCompleteTournament()
            val completeReportDEO = CompleteTournamentReportDEO.fromTournamentId(tournamentID)
            assert(completeReportDEO.tournament.id == tournamentID)
            transaction {
                val numGameReports = TournamentReport.find { TournamentReports.tournament eq tournamentID }
                    .filter { it.isSubmitted }
                    .map { it.gameReports.count() }
                    .sum()
                assert(completeReportDEO.games.size.toLong() == numGameReports)
            }
        }
    }

}
