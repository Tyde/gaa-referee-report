package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

fun TournamentDEO.Companion.fromTournament(input: Tournament): TournamentDEO {

    return transaction{TournamentDEO(
        input.id.value, input.name, input.location, input.date, input.region.id.value
    )}
}

fun TournamentDEO.updateInDatabase(): Result<Tournament> {
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


fun NewTournamentDEO.storeInDatabase(): Result<Tournament> {
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

fun RegionDEO.Companion.fromRegion(input: Region): RegionDEO {
    return RegionDEO(input.id.value, input.name)
}


fun PublicTournamentReportDEO.Companion.fromTournament(input: Tournament): PublicTournamentReportDEO {
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


fun PublicTournamentReportDEO.Companion.fromTournamentId(id:Long):PublicTournamentReportDEO {
    return transaction {
        val tournament = Tournament.findById(id)
        if (tournament == null) {
            throw IllegalArgumentException("Tournament with id $id does not exist")
        }
        fromTournament(tournament)
    }
}

fun CompleteTournamentReportDEO.Companion.fromTournament(input: Tournament): CompleteTournamentReportDEO {
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


fun CompleteTournamentReportDEO.Companion.fromTournamentId(id:Long):CompleteTournamentReportDEO {
    return transaction {
        val tournament = Tournament.findById(id)
        if (tournament == null) {
            throw IllegalArgumentException("Tournament with id $id does not exist")
        }
        fromTournament(tournament)
    }
}








