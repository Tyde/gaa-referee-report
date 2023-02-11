package eu.gaelicgames.referee.util

import eu.gaelicgames.referee.data.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

object MockPlayerList {

    data class MockPlayer(val firstName: String, val lastName: String, val details: String)
    val list = mutableListOf<MockPlayer>()
    init {
        val resource = this.javaClass.classLoader.getResourceAsStream("mock_data/players.csv")
        resource.use {
            val buf = it.bufferedReader()
            val csvParser = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(buf)
            for (record in csvParser) {
                val firstName = record.get(0)
                val lastName = record.get(1)
                val details = record.get(2)
                list.add(MockPlayer(firstName, lastName, details))
            }

        }
    }
}

object MockDataGenerator {
    fun addMockUsers() {
        val resource = this.javaClass.classLoader.getResourceAsStream("mock_data/users.csv")
        resource.use {
            it.bufferedReader().let { buf ->
                val csvParser = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(buf)
                println(csvParser.headerNames);
                transaction {
                    var firstCheckDone = false;
                    for (csvRecord in csvParser) {
                        if(!firstCheckDone) {
                            if(User.find { Users.mail eq csvRecord.get(4) }.count() > 0) {
                                println("Users already populated")
                                break
                            } else {
                                firstCheckDone = true
                            }
                        }
                        User.newWithPassword(
                            csvRecord.get(1),
                            csvRecord.get(2),
                            csvRecord.get(4),
                            csvRecord.get(3),
                            UserRole.REFEREE
                        )
                        println("Adding User: ${csvRecord.get(1)} ${csvRecord.get(2)} ${csvRecord.get(4)} ${csvRecord.get(3)}")
                    }
                }

            }
        }



    }

    fun addMockTournaments() {
        val resource = this.javaClass.classLoader.getResourceAsStream("mock_data/tournaments.csv")
        resource.use {
            it.bufferedReader().let { buf ->
                val csvParser = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(buf)
                transaction {
                    var firstCheckDone = false;
                    for (csvRecord in csvParser) {
                        if (!firstCheckDone) {
                            if (Tournament.find { Tournaments.name eq csvRecord.get(1) }.count() > 0) {
                                println("Tournaments already populated")
                                break
                            } else {
                                firstCheckDone = true
                            }
                        }
                        Tournament.new {
                            date = LocalDate.parse(csvRecord.get(0))
                            location = csvRecord.get(1)
                            name = csvRecord.get(2)
                        }
                        println(
                            "Adding Tournament: ${csvRecord.get(0)} ${csvRecord.get(1)} ${csvRecord.get(
                                2
                            )}"
                        )
                    }
                }
            }
        }
    }


    fun addMockTeams() {
        val resource = this.javaClass.classLoader.getResourceAsStream("mock_data/teams.csv")
        val csvParser = resource.use {
            it.bufferedReader().let { buf ->
                val csvParser = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(buf)
                transaction {
                    var firstCheckDone = false;
                    for (csvRecord in csvParser) {
                        if (!firstCheckDone) {
                            if (Team.find { Teams.name eq csvRecord.get(0) }.count() > 0) {
                                println("Teams already populated")
                                break
                            } else {
                                firstCheckDone = true
                            }
                        }
                        Team.new {
                            name = csvRecord.get(0)
                            isAmalgamation = false
                        }
                        println(
                            "Adding Team: ${csvRecord.get(0)}"
                        )
                    }
                }

            }
        }
    }


    fun addMockTournamentReport() {

        transaction {
            val referee = User.find { Users.role eq UserRole.REFEREE }.toList().random()
            val tournament = Tournament.all().toList().random()
            val completeTeamList = Team.all().toList()
            val teamList = completeTeamList.shuffled().take(Random.nextInt(2,16))
            val code = GameCode.all().toList().random()
            val report = TournamentReport.new {
                this.tournament = tournament
                this.referee = referee
                this.code = code
                this.isSubmitted = true
                this.submitDate = tournament.date.plusDays(1).atStartOfDay().plusHours(12)
            }
            for (team in teamList) {
                TournamentReportTeamPreSelection.new {
                    this.report = report
                    this.team = team
                }
            }
            var lastDateTime = tournament.date.atStartOfDay().plusHours(8).plusMinutes(Random.nextLong(1,45))
            for (i in 1..Random.nextInt(4, 16)) {
                lastDateTime = addMockGameReport(report,teamList,lastDateTime)
            }
            for (i in 1..Random.nextInt(1, 4)) {
                addMockPitchReport(report)
            }

        }

    }
    fun addMockGameReport(report:TournamentReport, teams:List<Team>, lastDateTime: LocalDateTime):LocalDateTime {
        return transaction {
            val teamA = teams.random()
            val teamB = teams.filter { it != teamA }.random()
            val startTime = lastDateTime.plusMinutes(Random.nextLong(20, 60))
            val gameType = GameType.all().toList().random()
            val teamAGoals = Random.nextInt(0, 6)
            val teamBGoals = Random.nextInt(0, 6)
            val teamAPoints = Random.nextInt(0,32)
            val teamBPoints = Random.nextInt(0,32)
            var extraTime = ExtraTimeOption.find{ ExtraTimeOptions.id eq 1L}.first()
            if(Random.nextInt(0,10)> 9){
                extraTime = ExtraTimeOption.all().toList().random()
            }
            val umpirePresentOnTime = Random.nextInt(0,10) < 8
            val gr = GameReport.new {
                this.report = report
                this.teamA = teamA
                this.teamB = teamB
                this.startTime = startTime
                this.gameType = gameType
                this.teamAGoals = teamAGoals
                this.teamBGoals = teamBGoals
                this.teamAPoints = teamAPoints
                this.teamBPoints = teamBPoints
                this.extraTime = extraTime
                this.umpirePresentOnTime = umpirePresentOnTime
                this.umpireNotes = "No Notes"
            }
            val numDisciplinaryActions = Random.nextInt(0, 4)
            (1..numDisciplinaryActions).forEach{
                addMockDisciplinaryAction(gr,report.code)
            }
            val numInjuries = Random.nextInt(0, 2)
            (1..numInjuries).forEach{
                addMockInjury(gr)
            }
            return@transaction startTime
        }
    }

    fun addMockDisciplinaryAction(gameReport: GameReport, code: GameCode) {
        transaction {
            val team = if(Random.nextBoolean())  gameReport.teamA else gameReport.teamB
            val player = MockPlayerList.list.random()
            val rule = Rule.find { Rules.code eq code.id }.toList().random()
            DisciplinaryAction.new {
                this.game = gameReport
                this.team = team
                this.firstName = player.firstName
                this.lastName = player.lastName
                this.number = Random.nextInt(1,34)
                this.rule = rule
                this.details = player.details
            }
        }
    }

    fun addMockInjury(gameReport: GameReport) {
        transaction {
            val team = if(Random.nextBoolean())  gameReport.teamA else gameReport.teamB
            val player = MockPlayerList.list.random()
            Injury.new {
                this.game = gameReport
                this.team = team
                this.firstName = player.firstName
                this.lastName = player.lastName
                this.details = player.details
            }
        }
    }

    fun addMockPitchReport(report: TournamentReport) {
        transaction {
            Pitch.new {
                this.report = report
                this.name = "Pitch "+Random.nextInt(1,20)
                this.surface = PitchSurfaceOption.all().toList().random()
                this.width = PitchWidthOption.all().toList().random()
                this.length = PitchLengthOption.all().toList().random()
                this.smallSquareMarkings = PitchMarkingsOption.all().toList().random()
                this.penaltySquareMarkings = PitchMarkingsOption.all().toList().random()
                this.thirteenMeterMarkings = PitchMarkingsOption.all().toList().random()
                this.twentyMeterMarkings = PitchMarkingsOption.all().toList().random()
                this.longMeterMarkings = PitchMarkingsOption.all().toList().random()
                this.goalPosts = PitchGoalpostsOption.all().toList().random()
                this.goalDimensions = PitchGoalDimensionOption.all().toList().random()
                this.additionalInformation = "Lorem ipsum"
            }
        }
    }
}