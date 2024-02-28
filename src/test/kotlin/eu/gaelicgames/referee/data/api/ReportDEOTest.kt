package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class ReportDEOTest {

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
    fun completeReportDEO_fromTournamentReport() {
        runBlocking {
            for (i in 1..5) {
                val gameReport = TestHelper.initializeGameReport(tournamentReportData)
                val (teamID, disciplinaryAction) = TestHelper.initializeDisciplinaryAction(gameReport!!)
                val (teamID2, injury) = TestHelper.initializeInjury(gameReport!!)
            }
            val reportDEO = CompleteReportDEO.fromTournamentReport(tournamentReportData.tournamentReport)
            assert(reportDEO != null)
            transaction {
                val games = GameReports.leftJoin(TournamentReports)
                    .select { TournamentReports.id eq tournamentReportData.tournamentReport.id.value }.count()
                assert(reportDEO.gameReports.size == games.toInt())
                assert(reportDEO.gameReports[0].disciplinaryActions.size == 1)
                assert(reportDEO.gameReports[0].injuries.size == 1)
                assert(reportDEO.tournament.id == tournamentReportData.tournamentReport.tournament.id.value)
                assert(reportDEO.tournament.name == tournamentReportData.tournamentReport.tournament.name)
                assert(reportDEO.selectedTeams.size == 5)
            }
        }
    }

    @Test
    fun tournamentReportByIdDEO_submit() {
        runBlocking {
            val tournamentReportData = TestHelper.setUpReport()
            val submitDEO = TournamentReportByIdDEO.fromTournamentReport(tournamentReportData.tournamentReport)
            val reply = submitDEO.submitInDatabase()
            assert(reply.isSuccess)
            val report = reply.getOrNull()
            assert(report != null)
            transaction {
                assert(report!!.id.value == tournamentReportData.tournamentReport.id.value)
                val submitted = TournamentReport.findById(tournamentReportData.tournamentReport.id.value)!!.isSubmitted
                assert(submitted)
            }
        }
    }

    @Test
    fun tournamentReportByIdDEO_createShareLink() {
        runBlocking {
            val submitDEO = TournamentReportByIdDEO.fromTournamentReport(tournamentReportData.tournamentReport)
            val reply = submitDEO.createShareLink()
            assert(reply.isSuccess)
            val shareLinkDEO = reply.getOrNull()
            assert(shareLinkDEO != null)
            transaction {
                val sharelinkDB = TournamentReportShareLink.find {
                    TournamentReportShareLinks.report eq tournamentReportData.tournamentReport.id
                }.first()
                assert(shareLinkDEO!!.uuid == sharelinkDB.uuid.toString())
            }
        }
    }

    @Test
    fun updateReportAdditionalInformationDEO_update() {
        runBlocking {
            val updateDEO = UpdateReportAdditionalInformationDEO(
                tournamentReportData.tournamentReportID, "New info")
            val reply = updateDEO.updateInDatabase()
            assert(reply.isSuccess)
            transaction {
                val updatedReport = TournamentReport.findById(tournamentReportData.tournamentReport.id.value)
                assert(updatedReport!!.additionalInformation == "New info")
            }
        }
    }

    @Test
    fun updateReportAdditionalInformationDEO_update_illegalId() {
        runBlocking {
            val firstIllegal = transaction {
                TournamentReport.all().map { it.id.value }.maxOrNull()?.plus(1) ?: 1
            }
            val updateDEO = UpdateReportAdditionalInformationDEO(
                firstIllegal, "New info")
            val reply = updateDEO.updateInDatabase()
            assert(reply.isFailure)
            assert(reply.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun updateReportAdditionalInformationDEO_update_emptyInfo() {
        runBlocking {
            val updateDEO = UpdateReportAdditionalInformationDEO(
                tournamentReportData.tournamentReportID, "")
            val reply = updateDEO.updateInDatabase()
            assert(reply.isFailure)
            assert(reply.exceptionOrNull() is IllegalArgumentException)
        }
    }



    @Test
    fun newTournamentReportDEO_create() {
        runBlocking {
            val tournamentID = transaction { Tournament.all().first().id.value }
            val gameCode = TestHelper.getGameCodeIDs().first()
            val newReportDEO = NewTournamentReportDEO(
                tournament = tournamentID,
                selectedTeams = tournamentReportData.teamIDs,
                gameCode = gameCode
            )
            val referee = TestHelper.getFirstUser()
            val reply = newReportDEO.storeInDatabase(referee)
            assert(reply != null)
            transaction {
                val report = TournamentReport.findById(reply!!.id)
                assert(report != null)
                assert(report!!.tournament.id.value == tournamentID)
                assert(report.referee.id.value == referee.id.value)
                assert(report.code.id.value == gameCode)
                assert(report.selectedTeams.count().toInt() == tournamentReportData.teamIDs.size)
            }

        }
    }

    @Test
    fun newTournamentReportDEO_create_illegalTournament() {
        runBlocking {
            val firstIllegal = transaction {
                Tournament.all().map { it.id.value }.maxOrNull()?.plus(1) ?: 1
            }
            val gameCode = TestHelper.getGameCodeIDs().first()
            val newReportDEO = NewTournamentReportDEO(
                tournament = firstIllegal,
                selectedTeams = tournamentReportData.teamIDs,
                gameCode = gameCode
            )
            val referee = TestHelper.getFirstUser()
            val reply = newReportDEO.storeInDatabase(referee)
            assert(reply == null)
        }
    }

    @Test
    fun newTournamentReportDEO_update() {
        runBlocking {
            val tournamentReportData = TestHelper.setUpReport()
            val newTournament = TestHelper.createTournament()
            val newReportDEO = NewTournamentReportDEO(
                id = tournamentReportData.tournamentReportID,
                tournament = newTournament.id.value,
                selectedTeams = tournamentReportData.teamIDs,
                gameCode = TestHelper.getGameCodeIDs().first()
            )
            val tournamentReport =newReportDEO.updateInDatabase()
            assert(tournamentReport.isSuccess)
            transaction {
                val report = TournamentReport.findById(tournamentReportData.tournamentReportID)
                assert(report!!.tournament.id.value == newTournament.id.value)
            }

        }
    }

    @Test
    fun newTournamentReportDEO_update_illegalTournament() {
        runBlocking {
            val firstIllegal = transaction {
                Tournament.all().map { it.id.value }.maxOrNull()?.plus(1) ?: 1
            }
            val newReportDEO = NewTournamentReportDEO(
                id = tournamentReportData.tournamentReportID,
                tournament = firstIllegal,
                selectedTeams = tournamentReportData.teamIDs,
                gameCode = TestHelper.getGameCodeIDs().first()
            )
            val reply = newReportDEO.updateInDatabase()
            assert(reply.isFailure)
            assert(reply.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun compactTournamentReportDEO_all() {
        runBlocking {
            val newReports = (1..5).map {
                val data = TestHelper.setUpReport()
                for (j in 1 ..3) {
                    TestHelper.initializeGameReportAndDisciplinaryAction(data)
                }
                data
            }
            val numReports = transaction { TournamentReport.all().count() }
            val compactReportDEO = CompactTournamentReportDEO.all()
            assert(compactReportDEO.size == numReports.toInt())
            transaction {
                val reports = TournamentReport.all().map { it.id.value }
                assert(compactReportDEO.map { it.id }.containsAll(reports))
                newReports.forEach { trr ->
                    val report = compactReportDEO.find { trr.tournamentReportID == it.id }
                    assert(report != null)
                    assert(report!!.numGameReports == 3L)
                    assert(report.numTeams == 5L)
                }
            }
        }
    }

    @Test
    fun compactTournamentReportDEO_fromTournamentReport() {
        runBlocking {
            val tournamentReportData = TestHelper.setUpReport()
            val gameReport = TestHelper.initializeGameReport(tournamentReportData)
            val compactReportDEO = CompactTournamentReportDEO.fromTournamentReport(tournamentReportData.tournamentReport)
            assert(compactReportDEO.numGameReports == 1L)
            assert(compactReportDEO.numTeams == 5L)
        }
    }


    @Test
    fun deleteTournamentReportDEO_delete() {
        runBlocking {
            val tournamentReportData = TestHelper.setUpReport()
            val deleteDEO = DeleteTournamentReportDEO(tournamentReportData.tournamentReportID)
            val reply = deleteDEO.deleteFromDatabase()
            assert(reply.isSuccess)
            transaction {
                val report = TournamentReport.findById(tournamentReportData.tournamentReportID)
                assert(report == null)
            }
        }
    }


}
