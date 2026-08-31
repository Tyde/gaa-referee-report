package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class TeamGamesDEOTest {

    companion object {
        private lateinit var fixtures: Fixtures

        @JvmStatic
        @BeforeAll
        fun setUp() {
            TestHelper.setupDatabase()
            fixtures = transaction { Fixtures.create() }
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            TestHelper.tearDownDatabase()
        }
    }

    @Test
    fun `returns submitted games oriented around the selected team with metadata`() = runBlocking {
        val result = TeamGamesDEO.forTeam(fixtures.alphaId, includeAmalgamatedTeams = false)

        assert(result.isSuccess) { "Expected a valid regular team to load" }
        val games = result.getOrThrow().games
        assert(games.map { it.gameId }.toSet() == setOf(fixtures.alphaAsTeamA, fixtures.alphaAsTeamB, fixtures.bothIncluded)) {
            "Expected only direct submitted games, got ${games.map { it.gameId }}"
        }

        val teamAGame = games.single { it.gameId == fixtures.alphaAsTeamA }
        assert(teamAGame.playedAsTeam.id == fixtures.alphaId)
        assert(teamAGame.opponentTeam.id == fixtures.betaId)
        assert(teamAGame.playedAsGoals == 2 && teamAGame.playedAsPoints == 4)
        assert(teamAGame.opponentGoals == 1 && teamAGame.opponentPoints == 8)
        assert(teamAGame.startTime == fixtures.firstStartTime)
        assert(teamAGame.gameTypeId == fixtures.finalTypeId && teamAGame.gameTypeName == "Final")
        assert(teamAGame.refereeId == fixtures.firstRefereeId && teamAGame.refereeName == "Ada Referee")
        assert(teamAGame.reportId == fixtures.firstReportId)
        assert(teamAGame.tournament.id == fixtures.firstTournamentId)
        assert(teamAGame.tournament.name == "Spring Tournament")
        assert(teamAGame.tournament.location == "Berlin")
        assert(teamAGame.codeId == fixtures.firstCodeId && teamAGame.codeName == "Hurling")

        val teamBGame = games.single { it.gameId == fixtures.alphaAsTeamB }
        assert(teamBGame.playedAsTeam.id == fixtures.alphaId)
        assert(teamBGame.opponentTeam.id == fixtures.gammaId)
        assert(teamBGame.playedAsGoals == 3 && teamBGame.playedAsPoints == 2)
        assert(teamBGame.opponentGoals == 1 && teamBGame.opponentPoints == 9)
        assert(teamBGame.startTime == null)
        assert(teamBGame.gameTypeId == null && teamBGame.gameTypeName == null)
        assert(teamBGame.refereeId == fixtures.secondRefereeId && teamBGame.refereeName == "Bea Official")
        assert(teamBGame.tournament.id == fixtures.secondTournamentId)

        val bothIncluded = games.single { it.gameId == fixtures.bothIncluded }
        assert(bothIncluded.playedAsTeam.id == fixtures.alphaId) {
            "The selected team must win when both game sides are included"
        }
        assert(games.count { it.gameId == fixtures.bothIncluded } == 1) { "A game must never be returned twice" }
    }

    @Test
    fun `excludes unsubmitted reports and team preselections without games`() = runBlocking {
        val games = TeamGamesDEO.forTeam(fixtures.alphaId, false).getOrThrow().games

        assert(games.none { it.gameId == fixtures.unsubmittedGame })
        assert(games.none { it.reportId == fixtures.preselectionOnlyReportId })
    }

    @Test
    fun `includes every current non-deleted amalgamation only when requested`() = runBlocking {
        val direct = TeamGamesDEO.forTeam(fixtures.alphaId, false).getOrThrow()
        val expanded = TeamGamesDEO.forTeam(fixtures.alphaId, true).getOrThrow()

        assert(direct.includedAmalgamations.isEmpty())
        assert(expanded.includedAmalgamations.map { it.id }.toSet() == setOf(fixtures.firstAmalgamationId, fixtures.secondAmalgamationId))
        assert(expanded.games.map { it.gameId }.toSet().containsAll(setOf(fixtures.firstAmalgamationGame, fixtures.secondAmalgamationGame)))
        assert(expanded.games.none { it.gameId == fixtures.deletedAmalgamationGame || it.gameId == fixtures.historyOnlyAmalgamationGame }) {
            "Only current non-deleted amalgamation rows may expand the selected club"
        }
    }

    @Test
    fun `selecting an amalgamation does not expand to its member clubs`() = runBlocking {
        val result = TeamGamesDEO.forTeam(fixtures.firstAmalgamationId, true)

        assert(result.isSuccess)
        val games = result.getOrThrow().games
        assert(result.getOrThrow().includedAmalgamations.isEmpty())
        assert(games.map { it.gameId }.toSet() == setOf(fixtures.firstAmalgamationGame, fixtures.bothIncluded))
        assert(games.none { it.gameId == fixtures.alphaAsTeamA || it.gameId == fixtures.alphaAsTeamB })
    }

    @Test
    fun `returns empty results for an active team without games and failures for missing or deleted teams`() = runBlocking {
        val empty = TeamGamesDEO.forTeam(fixtures.emptyTeamId, false)
        val missing = TeamGamesDEO.forTeam(Long.MAX_VALUE, false)
        val deleted = TeamGamesDEO.forTeam(fixtures.deletedTeamId, false)

        assert(empty.isSuccess && empty.getOrThrow().games.isEmpty())
        assert(missing.isFailure)
        assert(deleted.isFailure)
    }

    private data class Fixtures(
        val alphaId: Long,
        val betaId: Long,
        val gammaId: Long,
        val emptyTeamId: Long,
        val deletedTeamId: Long,
        val firstAmalgamationId: Long,
        val secondAmalgamationId: Long,
        val firstTournamentId: Long,
        val secondTournamentId: Long,
        val firstReportId: Long,
        val preselectionOnlyReportId: Long,
        val firstRefereeId: Long,
        val secondRefereeId: Long,
        val firstCodeId: Long,
        val finalTypeId: Long,
        val firstStartTime: LocalDateTime,
        val alphaAsTeamA: Long,
        val alphaAsTeamB: Long,
        val bothIncluded: Long,
        val firstAmalgamationGame: Long,
        val secondAmalgamationGame: Long,
        val deletedAmalgamationGame: Long,
        val historyOnlyAmalgamationGame: Long,
        val unsubmittedGame: Long
    ) {
        companion object {
            fun create(): Fixtures {
                fun team(name: String, isAmalgamation: Boolean = false, deleted: Boolean = false) = Team.new {
                    this.name = name
                    this.isAmalgamation = isAmalgamation
                    if (deleted) deletedAt = LocalDateTime.now()
                }
                val alpha = team("Alpha")
                val beta = team("Beta")
                val gamma = team("Gamma")
                val delta = team("Delta")
                val empty = team("Empty")
                val deleted = team("Deleted", deleted = true)
                val firstAmalgamation = team("Alpha Delta", isAmalgamation = true)
                val secondAmalgamation = team("Alpha Gamma", isAmalgamation = true)
                val deletedAmalgamation = team("Deleted Alpha Squad", isAmalgamation = true, deleted = true)
                val historyOnlyAmalgamation = team("Former Alpha Squad", isAmalgamation = true)
                Amalgamation.new { amalgamation = firstAmalgamation; addedTeam = alpha }
                Amalgamation.new { amalgamation = firstAmalgamation; addedTeam = delta }
                Amalgamation.new { amalgamation = secondAmalgamation; addedTeam = alpha }
                Amalgamation.new { amalgamation = secondAmalgamation; addedTeam = gamma }
                Amalgamation.new { amalgamation = deletedAmalgamation; addedTeam = alpha }
                TeamHistoryEvent.new {
                    team = historyOnlyAmalgamation
                    changeType = TeamChangeType.MEMBER_REMOVED
                    changeDate = LocalDate.now().minusYears(1)
                    oldValue = "Alpha"
                    newValue = null
                    recordedAt = LocalDateTime.now()
                    recordedBy = null
                }

                val firstReferee = referee("Ada", "Referee")
                val secondReferee = referee("Bea", "Official")
                val hurling = GameCode.new { name = "Hurling" }
                val football = GameCode.new { name = "Football" }
                val final = GameType.new { name = "Final" }
                val region = Region.all().first()
                val firstTournament = tournament("Spring Tournament", "Berlin", region)
                val secondTournament = tournament("Summer Tournament", "Munich", region)
                val firstReport = report(firstTournament, firstReferee, hurling, submitted = true)
                val secondReport = report(secondTournament, secondReferee, football, submitted = true)
                val unsubmittedReport = report(firstTournament, firstReferee, hurling, submitted = false)
                val preselectionOnlyReport = report(secondTournament, secondReferee, football, submitted = true)
                TournamentReportTeamPreSelection.new { report = preselectionOnlyReport; team = alpha }

                val start = LocalDateTime.of(2026, 5, 4, 10, 30)
                val alphaAsTeamA = game(firstReport, alpha, beta, 2, 4, 1, 8, start, final)
                val alphaAsTeamB = game(secondReport, gamma, alpha, 1, 9, 3, 2, null, null)
                val bothIncluded = game(firstReport, alpha, firstAmalgamation, 4, 0, 0, 6, start.plusHours(1), final)
                val firstAmalgamationGame = game(firstReport, firstAmalgamation, gamma, 1, 7, 0, 3, start.plusHours(2), final)
                val secondAmalgamationGame = game(secondReport, secondAmalgamation, beta, 2, 1, 1, 5, null, null)
                val deletedAmalgamationGame = game(firstReport, deletedAmalgamation, beta, 3, 0, 0, 1, start.plusHours(3), final)
                val historyOnlyAmalgamationGame = game(secondReport, historyOnlyAmalgamation, beta, 3, 3, 1, 0, null, null)
                val unsubmittedGame = game(unsubmittedReport, alpha, beta, 9, 9, 0, 0, start, final)

                return Fixtures(
                    alpha.id.value, beta.id.value, gamma.id.value, empty.id.value, deleted.id.value,
                    firstAmalgamation.id.value, secondAmalgamation.id.value, firstTournament.id.value,
                    secondTournament.id.value, firstReport.id.value, preselectionOnlyReport.id.value,
                    firstReferee.id.value, secondReferee.id.value, hurling.id.value, final.id.value, start,
                    alphaAsTeamA.id.value, alphaAsTeamB.id.value, bothIncluded.id.value,
                    firstAmalgamationGame.id.value, secondAmalgamationGame.id.value,
                    deletedAmalgamationGame.id.value, historyOnlyAmalgamationGame.id.value, unsubmittedGame.id.value
                )
            }

            private fun referee(firstName: String, lastName: String) = User.new {
                this.firstName = firstName
                this.lastName = lastName
                mail = "$firstName.$lastName@example.test"
                password = User.hashPassword("password")
                role = UserRole.REFEREE
            }

            private fun tournament(name: String, location: String, region: Region) = Tournament.new {
                this.name = name
                this.location = location
                date = LocalDate.of(2026, 5, 4)
                this.region = region
                isLeague = false
                endDate = null
            }

            private fun report(tournament: Tournament, referee: User, code: GameCode, submitted: Boolean) = TournamentReport.new {
                this.tournament = tournament
                this.referee = referee
                this.code = code
                additionalInformation = ""
                isSubmitted = submitted
            }

            private fun game(
                report: TournamentReport, teamA: Team, teamB: Team,
                teamAGoals: Int, teamAPoints: Int, teamBGoals: Int, teamBPoints: Int,
                startTime: LocalDateTime?, gameType: GameType?
            ) = GameReport.new {
                this.report = report
                this.teamA = teamA
                this.teamB = teamB
                this.teamAGoals = teamAGoals
                this.teamAPoints = teamAPoints
                this.teamBGoals = teamBGoals
                this.teamBPoints = teamBPoints
                this.startTime = startTime
                this.gameType = gameType
                umpirePresentOnTime = true
                generalNotes = ""
            }
        }
    }
}
