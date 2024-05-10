package eu.gaelicgames.referee.services

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.lockedTransaction
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SanitizeDataService(scope: CoroutineScope) : BaseRegularService(scope) {
    private val logger = LoggerFactory.getLogger(SanitizeDataService::class.java)

    override suspend fun runCycle() {
        logger.info("Sanitizing data")
        lockedTransaction {
            mergeTournamentReports()
            //fixDatesOnOneDayTournaments()
        }
    }




    private suspend fun fixDatesOnOneDayTournaments() {
        logger.info("Fixing dates on one day tournaments")
        lockedTransaction {
            GameReports.leftJoin(TournamentReports).leftJoin(Tournaments).selectAll()
                .where { TournamentReports.isSubmitted eq true and (Tournaments.isLeague eq false) }
                .groupBy {
                    it[TournamentReports.tournament].value
                }.forEach {map ->
                    map.value.forEach { row ->
                        val gameReportID = row[GameReports.id].value
                        val startDate = row[GameReports.startTime]
                        val tournamentDate = row[Tournaments.date]
                        if(startDate != null && startDate.toLocalDate() != tournamentDate) {
                            val newStartDate = startDate.withYear(tournamentDate.year)
                                .withMonth(tournamentDate.monthValue)
                                .withDayOfMonth(tournamentDate.dayOfMonth)
                            logger.info("Fixing date on game report $gameReportID from $startDate to $newStartDate")
                            GameReports.update({GameReports.id eq gameReportID}) {
                                it[startTime] = newStartDate
                            }
                        }

                    }
                }

        }
    }

    private suspend fun mergeTournamentReports() {
        logger.info("Looking for tournamentreports to merge")
        lockedTransaction {
            TournamentReports.selectAll()
                .where { TournamentReports.isSubmitted eq true }
                .groupBy {
                    Triple(
                        it[TournamentReports.tournament].value,
                        it[TournamentReports.referee].value,
                        it[TournamentReports.code].value
                    )
                }.values
                .forEach { resultRows ->
                    val tournamentReportIDs = resultRows.map { it[TournamentReports.id].value }
                    if (tournamentReportIDs.size > 1) {
                        logger.info("Found tournamentreports to merge: $tournamentReportIDs")
                        mergeTournamentReportList(tournamentReportIDs)
                    }
                }

        }
    }

    private suspend fun mergeTournamentReportList(tournamentReportIDs: List<Long>) {
        logger.info("Merging tournamentreports with IDs: $tournamentReportIDs")
        lockedTransaction {
            val mergeTarget = TournamentReport.findById(tournamentReportIDs.first())
            if (mergeTarget != null) {
                val targetSelectedTeams = mergeTarget.selectedTeams.toList()

                val mergeSources = tournamentReportIDs.drop(1)
                    .mapNotNull { TournamentReport.findById(it) }
                mergeSources.forEach { source ->
                    source.gameReports.forEach { gameReport ->
                        gameReport.report = mergeTarget
                    }
                    source.pitches.forEach { pitch ->
                        pitch.report = mergeTarget
                    }
                    val (teamsNotInTarget, teamsInTarget) = TournamentReportTeamPreSelections.selectAll().where {
                        TournamentReportTeamPreSelections.report eq source.id
                    }.partition {
                        //Split into two lists: The first contains all preselections that are not yet in the target
                        // the second list contains all preselections that are already in the target
                        targetSelectedTeams.find { team ->
                            it[TournamentReportTeamPreSelections.team] == team.id
                        } == null
                    }

                    // Update selections if not yet contained
                    TournamentReportTeamPreSelections.update({
                        TournamentReportTeamPreSelections.id inList
                                teamsNotInTarget.map { it[TournamentReportTeamPreSelections.id] }
                    }) {
                        it[report] = mergeTarget.id
                    }

                    // Remove selection if already contained
                    TournamentReportTeamPreSelections.deleteWhere {
                        TournamentReportTeamPreSelections.id inList
                                teamsInTarget.map { it[TournamentReportTeamPreSelections.id] }
                    }


                    if (source.submitDate?.isAfter(mergeTarget.submitDate) == true) {
                        mergeTarget.submitDate = source.submitDate
                    }

                    if(source.additionalInformation.isNotBlank()) {
                        val optionalLineSeparator = if(mergeTarget.additionalInformation.isNotBlank()) {
                            System.lineSeparator()
                        } else {
                            ""
                        }
                        mergeTarget.additionalInformation += optionalLineSeparator + source.additionalInformation
                    }
                    val sourceShareLinks = TournamentReportShareLink.find { TournamentReportShareLinks.report eq source.id }
                    sourceShareLinks.forEach { shareLink ->
                        shareLink.delete()
                    }
                    source.delete()

                }
            }
        }
    }

    suspend fun start() {
        this.start((4).hours, 1.minutes)
    }

}
