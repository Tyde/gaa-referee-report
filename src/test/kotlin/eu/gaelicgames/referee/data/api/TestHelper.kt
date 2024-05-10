package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.DatabaseHandler
import eu.gaelicgames.referee.util.USE_POSTGRES
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

object TestHelper {

    data class TournamentReportData(
        val tournamentReport: TournamentReport,
        val tournamentReportID: Long,
        val teamIDs: MutableList<Long>,
        val gameTypeIDs: MutableList<Long>,
        val extraTimeIDs: MutableList<Long>
    )


    fun setupDatabase() {
        runBlocking {
            DatabaseHandler.init(testing = true)
            DatabaseHandler.createSchema()
            DatabaseHandler.populate_base_data()
        }
    }

    fun tearDownDatabase() {
        if (USE_POSTGRES) {
            //Remove all data from the database
            transaction {
                exec("DROP SCHEMA public CASCADE;")
                exec("CREATE SCHEMA public;")
                exec("GRANT ALL ON SCHEMA public TO root;")
                exec("GRANT ALL ON SCHEMA public TO public;")
            }
        } else {
            File("data/test.db").delete()
        }

    }
    fun setUpReport(
        preselectedTournament: Tournament? = null,
        preselectedUser: User? = null,
        preselectedTeams: List<Team> = emptyList()
    ):TournamentReportData {

        var tournamentReport: TournamentReport? = null
        var tournamentReportID: Long = 0
        val teamIDs = mutableListOf<Long>()
        val gameTypeIDs = mutableListOf<Long>()
        val extraTimeIDs = mutableListOf<Long>()
        return transaction {
            GameType.all().forEach { gameTypeIDs.add(it.id.value) }
            ExtraTimeOption.all().forEach { extraTimeIDs.add(it.id.value) }
            val firstRegion = Region.all().first()

            val tournament = preselectedTournament
                ?: Tournament.new {
                    name = "Test Tournament"
                    date = LocalDate.now()
                    region = firstRegion
                    location = "Test Location"
                }
            val referee = preselectedUser ?: User.new {
                this.firstName = "Test"
                this.lastName = "Referee"
                this.mail = "abc@def.de"
                this.password = User.hashPassword("123")
                this.role = UserRole.REFEREE
            }
            val code = GameCode.all().first()
            tournamentReport = TournamentReport.new {
                this.tournament = tournament
                this.referee = referee
                this.code = code
                additionalInformation = ""
                isSubmitted = false
            }
            tournamentReportID = tournamentReport!!.id.value
            if (preselectedTeams.isEmpty()) {
                for (i in 1..5) {
                    val team = Team.new {
                        name = "Team $i"
                        isAmalgamation = false
                    }
                    TournamentReportTeamPreSelection.new {
                        this.team = team
                        this.report = tournamentReport!!
                    }
                    teamIDs.add(team.id.value)
                }
            } else {
                preselectedTeams.forEach {
                    TournamentReportTeamPreSelection.new {
                        this.team = it
                        this.report = tournamentReport!!
                    }
                    teamIDs.add(it.id.value)
                }
            }

            return@transaction TournamentReportData(
                tournamentReport!!,
                tournamentReportID,
                teamIDs,
                gameTypeIDs,
                extraTimeIDs
            )
        }
    }

    suspend fun initializeGameReport(tournamentReportData: TournamentReportData): GameReport? {
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
        assert(result.isSuccess, { "GameReport creation failed: ${result.exceptionOrNull()}" })
        val gameReport = result.getOrNull()
        assert(gameReport != null)
        return gameReport
    }

    fun initializeDisciplinaryAction(gameReport: GameReport): Pair<Long, DisciplinaryAction> {
        val teamID = transaction {
            gameReport!!.teamA.id.value
        }
        val disciplinaryAction = transaction {
            DisciplinaryAction.new {
                this.team = Team.findById(teamID)!!
                this.firstName = "Test"
                this.lastName = "Player"
                this.number = 1
                this.details = "Test"
                this.rule = Rule.all().first()
                this.game = gameReport!!
                this.redCardIssued = true
            }
        }
        return Pair(teamID, disciplinaryAction)
    }

    fun initializeInjury(gameReport: GameReport): Pair<Long,Injury> {
        val teamID = transaction {
            gameReport!!.teamA.id.value
        }
        val injury = transaction {
            Injury.new {
                this.team = Team.findById(teamID)!!
                this.firstName = "Test"
                this.lastName = "Player"
                this.details = "Test"
                this.game = gameReport!!
            }
        }
        return Pair(teamID, injury)
    }

    suspend fun initializeGameReportAndDisciplinaryAction(tournamentReportData: TournamentReportData): Triple<GameReport,Long,DisciplinaryAction> {
        val gameReport = initializeGameReport(tournamentReportData)
        val (teamID, disciplinaryAction) = initializeDisciplinaryAction(gameReport!!)
        return Triple(gameReport,teamID,disciplinaryAction)
    }

    suspend fun initializeGameReportAndInjury(tournamentReportData: TournamentReportData): Triple<GameReport,Long,Injury> {
        val gameReport = initializeGameReport(tournamentReportData)
        val (teamID, injury) = initializeInjury(gameReport!!)
        return Triple(gameReport,teamID,injury)
    }


    fun createTournament():Tournament {
        return transaction {
            val firstRegion = Region.all().first()
            Tournament.new {
                name = "Test Tournament"
                date = LocalDate.now()
                region = firstRegion
                location = "Test Location"
            }
        }
    }


    fun getRulesIDs(): List<Long> {
        return transaction {
            Rule.all().toList().map { it.id.value }
        }
    }

    fun getGameCodeIDs(): List<Long> {
        return transaction {
            GameCode.all().toList().map { it.id.value }
        }
    }

    fun getFirstUser(): User {
        return transaction {
            User.all().first()
        }
    }
}
