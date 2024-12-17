package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.DatabaseHandler
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

internal class GameReportDEOTest {

    @AfterEach
    fun tearDown() {
    }

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
    fun gameReportDEO_create() {
        runBlocking {
            val deo = GameReportDEO(
                report = tournamentReportData.tournamentReportID,
                teamA = tournamentReportData.teamIDs[0],
                teamB = tournamentReportData.teamIDs[1],
                teamAGoals = 2,
                teamBGoals = 1,
                teamAPoints = 3,
                teamBPoints = 4,
                startTime = LocalDateTime.now(),
                gameType = tournamentReportData.gameTypeIDs[0],
                extraTime = tournamentReportData.extraTimeIDs[0],
                umpirePresentOnTime = true,
                umpireNotes = "abv",
                generalNotes = "def"
            )
            val result = deo.createInDatabase()
            assert(result.isSuccess)
            val gameReport = result.getOrNull()
            assert(gameReport != null)
            assert(gameReport!!.id.value > 0)

            transaction {
                val testVal = GameReport.findById(gameReport.id.value)
                assert(testVal != null)
                assert(testVal!!.teamA.id.value == tournamentReportData.teamIDs[0])
                assert(testVal.teamB.id.value == tournamentReportData.teamIDs[1])
                assert(testVal.teamAGoals == 2)
                assert(testVal.teamBGoals == 1)
                assert(testVal.teamAPoints == 3)
                assert(testVal.teamBPoints == 4)
                assert(testVal.startTime != null)
                assert(testVal.gameType?.id?.value == tournamentReportData.gameTypeIDs[0])
                assert(testVal.extraTime?.id?.value == tournamentReportData.extraTimeIDs[0])
                assert(testVal.umpirePresentOnTime)
                assert(testVal.umpireNotes == "abv")
                assert(testVal.generalNotes == "def")
            }
        }


    }

    @Test
    fun gameReportDEO_create_missing_report() {
        runBlocking {
            val deo = GameReportDEO(
                teamA = tournamentReportData.teamIDs[0],
                teamB = tournamentReportData.teamIDs[1],
                teamAGoals = 2,
                teamBGoals = 1,
                teamAPoints = 3,
                teamBPoints = 4,
                startTime = LocalDateTime.now(),
                gameType = tournamentReportData.gameTypeIDs[0],
                extraTime = tournamentReportData.extraTimeIDs[0],
                umpirePresentOnTime = true,
                umpireNotes = "abv",
                generalNotes = "def"
            )
            val result = deo.createInDatabase()
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun gameReportDEO_create_invalid_report() {
        runBlocking {
            val firstInvalid = transaction {
                TournamentReport.all().map { it.id.value }.maxOrNull()?.plus(1) ?: 1
            }
            val deo = GameReportDEO(
                report = firstInvalid,
                teamA = tournamentReportData.teamIDs[0],
                teamB = tournamentReportData.teamIDs[1],
                teamAGoals = 2,
                teamBGoals = 1,
                teamAPoints = 3,
                teamBPoints = 4,
                startTime = LocalDateTime.now(),
                gameType = tournamentReportData.gameTypeIDs[0],
                extraTime = tournamentReportData.extraTimeIDs[0],
                umpirePresentOnTime = true,
                umpireNotes = "abv",
                generalNotes = "def"
            )
            val result = deo.createInDatabase()
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun gameReportDEO_create_with_id_specified() {
        runBlocking {
            val deo = GameReportDEO(
                id = 1,
                report = tournamentReportData.tournamentReportID,
                teamA = tournamentReportData.teamIDs[0],
                teamB = tournamentReportData.teamIDs[1],
                teamAGoals = 2,
                teamBGoals = 1,
                teamAPoints = 3,
                teamBPoints = 4,
                startTime = LocalDateTime.now(),
                gameType = tournamentReportData.gameTypeIDs[0],
                extraTime = tournamentReportData.extraTimeIDs[0],
                umpirePresentOnTime = true,
                umpireNotes = "abv",
                generalNotes = "def"
            )
            val result = deo.createInDatabase()
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun gameReportDEO_update() {
        runBlocking {
            val gameReport = TestHelper.initializeGameReport(tournamentReportData)

            var updateDEO = GameReportDEO(
                id = gameReport!!.id.value,
                teamA = tournamentReportData.teamIDs[2],
            )
            var updateResult = updateDEO.updateInDatabase()
            assert(updateResult.isSuccess)
            var updatedGameReport = updateResult.getOrNull()
            assert(updatedGameReport != null)
            transaction {
                assert(updatedGameReport!!.id.value == gameReport.id.value)
                assert(updatedGameReport!!.teamA.id.value == tournamentReportData.teamIDs[2])
            }

            updateDEO = GameReportDEO(
                id = gameReport.id.value,
                teamB = tournamentReportData.teamIDs[3],
            )
            updateResult = updateDEO.updateInDatabase()
            assert(updateResult.isSuccess)
            updatedGameReport = updateResult.getOrNull()
            assert(updatedGameReport != null)
            transaction {
                assert(updatedGameReport!!.id.value == gameReport.id.value)
                assert(updatedGameReport!!.teamB.id.value == tournamentReportData.teamIDs[3])
            }

            updateDEO = GameReportDEO(
                id = gameReport.id.value,
                teamAGoals = 3,
            )
            updateResult = updateDEO.updateInDatabase()
            assert(updateResult.isSuccess)
            updatedGameReport = updateResult.getOrNull()
            assert(updatedGameReport != null)
            transaction {
                assert(updatedGameReport!!.id.value == gameReport.id.value)
                assert(updatedGameReport!!.teamAGoals == 3)
            }

            val startTime = LocalDateTime.now().minusDays(1)
            updateDEO = GameReportDEO(
                id = gameReport.id.value,
                startTime = startTime,
            )
            updateResult = updateDEO.updateInDatabase()
            assert(updateResult.isSuccess)
            updatedGameReport = updateResult.getOrNull()
            assert(updatedGameReport != null)
            transaction {
                assert(updatedGameReport!!.id.value == gameReport.id.value)
                assert(updatedGameReport.startTime!!.isEqual(startTime))
            }


        }
    }


    @Test
    fun gameReportDEO_update_no_id() {
        runBlocking {
            val updateDEO = GameReportDEO(
                teamA = tournamentReportData.teamIDs[2],
            )
            val updateResult = updateDEO.updateInDatabase()
            assert(updateResult.isFailure)
            assert(updateResult.exceptionOrNull() is IllegalArgumentException)

        }
    }

    @Test
    fun gameReport_update_invalid_id() {
        runBlocking {
            val grRealIds = transaction {
                GameReport.all().map { it.id.value }
            }
            val firstInvalid = grRealIds.maxOrNull()?.plus(1) ?: 1
            val updateDEO = GameReportDEO(
                id = firstInvalid,
                teamA = tournamentReportData.teamIDs[2],
            )
            val updateResult = updateDEO.updateInDatabase()
            assert(updateResult.isFailure)
            assert(updateResult.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun deleteGameReportDEO_delete() {
        runBlocking {
            val gameReport = TestHelper.initializeGameReport(tournamentReportData)
            val deleteDEO = DeleteGameReportDEO(
                id = gameReport!!.id.value
            )
            val deleteResult = deleteDEO.deleteFromDatabase()
            assert(deleteResult.isSuccess)
            transaction {
                val testVal = GameReport.findById(gameReport.id.value)
                assert(testVal == null)
            }
        }
    }

    @Test
    fun deleteGameReportDEO_delete_no_id() {
        runBlocking {
            val deleteDEO = DeleteGameReportDEO()
            val deleteResult = deleteDEO.deleteFromDatabase()
            assert(deleteResult.isFailure)
            assert(deleteResult.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun deleteGameReportDEO_delete_invalid_d() {
        runBlocking {
            val grRealIds = transaction {
                GameReport.all().map { it.id.value }
            }
            val firstInvalid = grRealIds.maxOrNull()?.plus(1) ?: 1
            val deleteDEO = DeleteGameReportDEO(
                id = firstInvalid
            )
            val deleteResult = deleteDEO.deleteFromDatabase()
            assert(deleteResult.isFailure)
            assert(deleteResult.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun gameReportClassesDEO_get() {
        runBlocking {
            val deo = GameReportClassesDEO.load()
            assert(deo.extraTimeOptions.isNotEmpty())
            assert(deo.gameTypes.isNotEmpty())
        }
    }

    @Test
    fun disciplinaryActionDEO_create() {
        runBlocking {
            val gameReport = TestHelper.initializeGameReport(tournamentReportData)
            val teamID = transaction {
                gameReport!!.teamA.id.value
            }
            val rules = TestHelper.getRulesIDs()
            val deo = DisciplinaryActionDEO(
                team = teamID,
                firstName = "Test",
                lastName = "Player",
                number = 1,
                details = "Test",
                rule = rules[0],
                game = gameReport!!.id.value,
                redCardIssued = true
            )
            val result = deo.createInDatabase()
            assert(result.isSuccess)
            val disciplinaryAction = result.getOrNull()
            assert(disciplinaryAction != null)
            assert(disciplinaryAction!!.id.value > 0)

            transaction {
                val testVal = DisciplinaryAction.findById(disciplinaryAction.id.value)
                assert(testVal != null)
                assert(testVal!!.team.id.value == teamID)
                assert(testVal.firstName == "Test")
                assert(testVal.lastName == "Player")
                assert(testVal.number == 1)
                assert(testVal.details == "Test")
                assert(testVal.rule.id.value == rules[0])
                assert(testVal.game.id.value == gameReport.id.value)
                assert(testVal.redCardIssued)
            }
        }
    }


    @Test
    fun disciplinaryActionDEO_create_no_game_report() {
        runBlocking {
            val teamID = transaction {
                Team.all().first().id.value
            }
            val rules = TestHelper.getRulesIDs()
            val deo = DisciplinaryActionDEO(
                team = teamID,
                firstName = "Test",
                lastName = "Player",
                number = 1,
                details = "Test",
                rule = rules[0],
                redCardIssued = true
            )
            val result = deo.createInDatabase()
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }
    @Test
    fun disciplinaryActionDEO_create_missing_team() {
        runBlocking {
            val gameReport = TestHelper.initializeGameReport(tournamentReportData)
            val rules = TestHelper.getRulesIDs()
            val deo = DisciplinaryActionDEO(
                firstName = "Test",
                lastName = "Player",
                number = 1,
                details = "Test",
                rule = rules[0],
                game = gameReport!!.id.value,
                redCardIssued = true
            )
            val result = deo.createInDatabase()
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun disciplinaryActionDEO_create_empty_name() {
        runBlocking {
            val gameReport = TestHelper.initializeGameReport(tournamentReportData)
            val teamID = transaction {
                gameReport!!.teamA.id.value
            }
            val rules = TestHelper.getRulesIDs()
            val deo = DisciplinaryActionDEO(
                team = teamID,
                firstName = "",
                lastName = "",
                number = 1,
                details = "Test",
                rule = rules[0],
                game = gameReport!!.id.value,
                redCardIssued = true
            )
            val result = deo.createInDatabase()
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun disciplinaryActionDEO_create_invalid_rule() {
        runBlocking {
            val gameReport = TestHelper.initializeGameReport(tournamentReportData)
            val teamID = transaction {
                gameReport!!.teamA.id.value
            }
            val rules = TestHelper.getRulesIDs()
            val firstInvalidRule = rules.maxOrNull()?.plus(1) ?: 1
            val deo = DisciplinaryActionDEO(
                team = teamID,
                firstName = "Test",
                lastName = "Player",
                number = 1,
                details = "Test",
                rule = firstInvalidRule,
                game = gameReport!!.id.value,
                redCardIssued = true
            )
            val result = deo.createInDatabase()
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun disciplinaryActionDEO_update() {
        runBlocking {
            val (gameReport, teamID, disciplinaryAction) = TestHelper.initializeGameReportAndDisciplinaryAction(
                tournamentReportData
            )
            val updateDEO = DisciplinaryActionDEO(
                id = disciplinaryAction.id.value,
                firstName = "Test2",
                lastName = "Player2",

            )
            val updateResult = updateDEO.updateInDatabase()
            assert(updateResult.isSuccess)
            val updatedDisciplinaryAction = updateResult.getOrNull()
            assert(updatedDisciplinaryAction != null)
            transaction {
                assert(updatedDisciplinaryAction!!.id.value == disciplinaryAction.id.value)
                assert(updatedDisciplinaryAction.firstName == "Test2")
                assert(updatedDisciplinaryAction.lastName == "Player2")
                assert(updatedDisciplinaryAction.rule.id.value == disciplinaryAction.rule.id.value)
            }
        }
    }

    @Test
    fun disciplinaryActionDEO_update_empty_name() {
        runBlocking {
            val (gameReport, teamID, disciplinaryAction) = TestHelper.initializeGameReportAndDisciplinaryAction(
                tournamentReportData
            )
            val updateDEO = DisciplinaryActionDEO(
                id = disciplinaryAction.id.value,
                firstName = "",

            )
            val updateResult = updateDEO.updateInDatabase()
            assert(updateResult.isSuccess)
            val updatedDisciplinaryAction = updateResult.getOrNull()
            assert(updatedDisciplinaryAction != null)
            transaction {
                assert(updatedDisciplinaryAction!!.id.value == disciplinaryAction.id.value)
                assert(updatedDisciplinaryAction.firstName == disciplinaryAction.firstName)
                assert(updatedDisciplinaryAction.lastName == "Player")
                assert(updatedDisciplinaryAction.rule.id.value == disciplinaryAction.rule.id.value)
            }
        }
    }

    @Test
    fun disciplinaryActionDEO_update_no_id() {
        runBlocking {
            val updateDEO = DisciplinaryActionDEO(
                firstName = "Test2",
                lastName = "Player2",

            )
            val updateResult = updateDEO.updateInDatabase()
            assert(updateResult.isFailure)
            assert(updateResult.exceptionOrNull() is IllegalArgumentException)
        }
    }


    @Test
    fun disciplinaryActionDEO_update_invalid_id() {
        runBlocking {
            val (gameReport, teamID, disciplinaryAction) = TestHelper.initializeGameReportAndDisciplinaryAction(
                tournamentReportData
            )
            val daRealIds = transaction {
                DisciplinaryAction.all().map { it.id.value }
            }
            val firstInvalid = daRealIds.maxOrNull()?.plus(1) ?: 1
            val updateDEO = DisciplinaryActionDEO(
                id = firstInvalid,
                firstName = "Test2",
                lastName = "Player2",

            )
            val updateResult = updateDEO.updateInDatabase()
            assert(updateResult.isFailure)
            assert(updateResult.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun disciplinaryActionDEO_update_invalid_rule() {
        runBlocking {
            val (gameReport, teamID, disciplinaryAction) = TestHelper.initializeGameReportAndDisciplinaryAction(
                tournamentReportData
            )
            val rules = TestHelper.getRulesIDs()
            val firstInvalidRule = rules.maxOrNull()?.plus(1) ?: 1
            val updateDEO = DisciplinaryActionDEO(
                id = disciplinaryAction.id.value,
                rule = firstInvalidRule,
            )
            val updateResult = updateDEO.updateInDatabase()
            assert(updateResult.isSuccess)
            val updatedDisciplinaryAction = updateResult.getOrNull()
            assert(updatedDisciplinaryAction != null)
            transaction {
                assert(updatedDisciplinaryAction!!.id.value == disciplinaryAction.id.value)
                assert(updatedDisciplinaryAction.rule.id.value == disciplinaryAction.rule.id.value)
            }
        }
    }


    @Test
    fun deleteDisciplinaryActionDEO_delete() {
        runBlocking {
            val (gameReport, teamID, disciplinaryAction) = TestHelper.initializeGameReportAndDisciplinaryAction(
                tournamentReportData
            )
            val deleteDEO = DeleteDisciplinaryActionDEO(
                id = disciplinaryAction.id.value
            )
            val deleteResult = deleteDEO.deleteFromDatabase()
            assert(deleteResult.isSuccess)
            transaction {
                val testVal = DisciplinaryAction.findById(disciplinaryAction.id.value)
                assert(testVal == null)
            }
        }
    }

    @Test
    fun deleteDisciplinaryActionDEO_delete_invalid_id() {
        runBlocking {
            val firstInvalid = transaction {
                DisciplinaryAction.all().map { it.id.value }.maxOrNull()?.plus(1) ?: 1
            }
            val deleteDEO = DeleteDisciplinaryActionDEO(
                id = firstInvalid
            )
            val deleteResult = deleteDEO.deleteFromDatabase()
            assert(deleteResult.isFailure)
            assert(deleteResult.exceptionOrNull() is IllegalArgumentException)
        }

    }


    @Test
    fun injuryDEO_create() {
        runBlocking {
            val gameReport = TestHelper.initializeGameReport(tournamentReportData)
            val teamID = transaction {
                gameReport!!.teamA.id.value
            }
            val deo = InjuryDEO(
                team = teamID,
                firstName = "Test",
                lastName = "Player",
                details = "Test",
                game = gameReport!!.id.value,
            )
            val result = deo.createInDatabase()
            assert(result.isSuccess)
            val injury = result.getOrNull()
            assert(injury != null)
            assert(injury!!.id.value > 0)

            transaction {
                val testVal = Injury.findById(injury.id.value)
                assert(testVal != null)
                assert(testVal!!.team.id.value == teamID)
                assert(testVal.firstName == "Test")
                assert(testVal.lastName == "Player")
                assert(testVal.details == "Test")
                assert(testVal.game.id.value == gameReport.id.value)
            }
        }
    }

    @Test
    fun completeGameReportDEO_load() {
        runBlocking {
            val (gameReport, teamID, disciplinaryAction) = TestHelper.initializeGameReportAndDisciplinaryAction(
                tournamentReportData
            )
            val deo = CompleteGameReportDEO.fromGameReport(gameReport!!)
            assert(deo != null)
            transaction {
                assert(deo!!.gameReport.id == gameReport.id.value)
                assert(deo.gameReport.teamA == gameReport.teamA.id.value)
                assert(deo.gameReport.teamB == gameReport.teamB.id.value)
                assert(deo.gameReport.teamAGoals == gameReport.teamAGoals)
                assert(deo.gameReport.teamBGoals == gameReport.teamBGoals)
                assert(deo.gameReport.teamAPoints == gameReport.teamAPoints)
                assert(deo.gameReport.teamBPoints == gameReport.teamBPoints)
                assert(deo.gameReport.startTime == gameReport.startTime)
                assert(deo.gameReport.gameType == gameReport.gameType?.id?.value)
                assert(deo.gameReport.extraTime == gameReport.extraTime?.id?.value)
                assert(deo.gameReport.umpirePresentOnTime == gameReport.umpirePresentOnTime)
                assert(deo.gameReport.umpireNotes == gameReport.umpireNotes)
                assert(deo.gameReport.generalNotes == gameReport.generalNotes)
                assert(deo.disciplinaryActions.isNotEmpty())
                assert(deo.injuries.isEmpty())
            }

        }

    }
}
