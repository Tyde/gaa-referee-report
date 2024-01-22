package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate


@Serializable
data class TournamentDEO(
    val id: Long,
    val name: String,
    val location: String,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val region: Long
) {
    companion object {
        fun fromTournament(input: Tournament): TournamentDEO {

            return transaction{TournamentDEO(
                input.id.value, input.name, input.location, input.date, input.region.id.value
            )}
        }
    }

    fun updateInDatabase(): Result<Tournament> {
        val thisDEO = this
        return transaction {
            val region = Region.findById(thisDEO.region)
            if (region == null) {
                return@transaction Result.failure(
                    IllegalArgumentException("Region with id ${thisDEO.region} does not exist")
                )
            }
            val tournament = Tournament.findById(thisDEO.id)
            if (tournament == null) {
                return@transaction Result.failure(
                    IllegalArgumentException("Tournament with id ${thisDEO.id} does not exist")
                )
            }
            tournament.name = thisDEO.name
            tournament.location = thisDEO.location
            tournament.date = thisDEO.date
            tournament.region = region
            return@transaction Result.success(tournament)
        }
    }
}


@Serializable
data class NewTournamentDEO(
    val name: String,
    val location: String,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val region: Long
) {
    fun storeInDatabase(): Result<Tournament> {
        val thisDEO = this
        return transaction {
            val region = Region.findById(thisDEO.region)
            if (region == null) {
                return@transaction Result.failure(
                    IllegalArgumentException("Region with id ${thisDEO.region} does not exist")
                )
            }
            val tournament = Tournament.new {
                name = thisDEO.name
                location = thisDEO.location
                date = thisDEO.date
                this.region = region
            }
            return@transaction Result.success(tournament)
        }
    }
}


@Serializable
data class RegionDEO(
    val id: Long,
    val name: String
) {
    companion object {
        fun fromRegion(input: Region): RegionDEO {
            return RegionDEO(input.id.value, input.name)
        }
    }
}

@Serializable
data class PublicTournamentReportDEO(
    val tournament: TournamentDEO,
    val games: List<PublicGameReportDEO>,
    val teams: List<TeamDEO>
) {
    companion object {
        fun fromTournament(input: Tournament): PublicTournamentReportDEO {
            return transaction {

                val gameReports = TournamentReports.leftJoin(GameReports).select {
                    (TournamentReports.tournament eq input.id) and
                            (TournamentReports.isSubmitted eq true)
                }.map {
                    TournamentReport.wrapRow(it)
                    val gameReport = GameReport.wrapRow(it)
                    PublicGameReportDEO.fromGameReport(gameReport)
                }

                val allTeams = gameReports
                    .flatMap { listOf(it.gameReport.teamA, it.gameReport.teamB)}
                    .asSequence()
                    .distinct()
                    .filterNotNull()
                    .map { TeamDEO.fromTeamId(it) }
                    .filter { it.isSuccess }
                    .map { it.getOrThrow() }
                    .toList()

                PublicTournamentReportDEO(
                    TournamentDEO.fromTournament(input),
                    gameReports,
                    allTeams
                )
            }

        }

        fun fromTournamentId(id:Long):PublicTournamentReportDEO {
            return transaction {
                val tournament = Tournament.findById(id)
                if (tournament == null) {
                    throw IllegalArgumentException("Tournament with id $id does not exist")
                }
                fromTournament(tournament)
            }
        }
    }
}

@Serializable
data class CompleteTournamentReportDEO(
    val tournament: TournamentDEO,
    val games: List<CompleteGameReportWithRefereeReportDEO>,
    val teams: List<TeamDEO>
) {
    companion object {
        fun fromTournament(input: Tournament): CompleteTournamentReportDEO {
            return transaction {

                val gameReports = TournamentReports.leftJoin(GameReports).select {
                    (TournamentReports.tournament eq input.id) and
                            (TournamentReports.isSubmitted eq true)
                }.map {
                    TournamentReport.wrapRow(it)
                    val gameReport = GameReport.wrapRow(it)
                    CompleteGameReportWithRefereeReportDEO.fromGameReport(gameReport)
                }

                val allTeams = gameReports
                    .flatMap { listOf(it.gameReport.gameReport.teamA, it.gameReport.gameReport.teamB) }
                    .asSequence()
                    .distinct()
                    .filterNotNull()
                    .map { TeamDEO.fromTeamId(it) }
                    .filter { it.isSuccess }
                    .map { it.getOrThrow() }
                    .toList()

                CompleteTournamentReportDEO(
                    TournamentDEO.fromTournament(input),
                    gameReports,
                    allTeams
                )
            }
        }

        fun fromTournamentId(id:Long):CompleteTournamentReportDEO {
            return transaction {
                val tournament = Tournament.findById(id)
                if (tournament == null) {
                    throw IllegalArgumentException("Tournament with id $id does not exist")
                }
                fromTournament(tournament)
            }
        }
    }
}




