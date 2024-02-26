package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.CacheUtil
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction

fun TournamentDEO.Companion.fromTournament(input: Tournament): TournamentDEO {

    return transaction {
        TournamentDEO(
            input.id.value, input.name, input.location, input.date, input.region.id.value
        )
    }
}

suspend fun TournamentDEO.updateInDatabase(): Result<Tournament> {
    val thisDEO = this
    return newSuspendedTransaction {
        val region = Region.findById(thisDEO.region)
        if (region == null) {
            return@newSuspendedTransaction Result.failure(
                IllegalArgumentException("Region with id ${thisDEO.region} does not exist")
            )
        }
        val tournament = Tournament.findById(thisDEO.id)
        if (tournament == null) {
            return@newSuspendedTransaction Result.failure(
                IllegalArgumentException("Tournament with id ${thisDEO.id} does not exist")
            )
        }
        CacheUtil.deleteCachedCompleteTournamentReport(tournament.id.value)
        CacheUtil.deleteCachedPublicTournamentReport(tournament.id.value)
        TournamentReport.find { TournamentReports.tournament eq tournament.id }.forEach {
            CacheUtil.deleteCachedReport(it.id.value)
        }

        tournament.name = thisDEO.name
        tournament.location = thisDEO.location
        tournament.date = thisDEO.date
        tournament.region = region
        return@newSuspendedTransaction Result.success(tournament)
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


suspend fun PublicTournamentReportDEO.Companion.fromTournament(input: Tournament): PublicTournamentReportDEO {
    return suspendedTransactionAsync {

        val gameReports = TournamentReports.leftJoin(GameReports).select {
            (TournamentReports.tournament eq input.id) and
                    (TournamentReports.isSubmitted eq true)
        }.map {
            TournamentReport.wrapRow(it)
            val gameReport = GameReport.wrapRow(it)
            PublicGameReportDEO.fromGameReport(gameReport)
        }

        val allTeams = gameReports
            .flatMap { listOf(it.gameReport.teamA, it.gameReport.teamB) }
            .asSequence()
            .distinct()
            .filterNotNull()
            .map { TeamDEO.fromTeamId(it) }
            .filter { it.isSuccess }
            .map { it.getOrThrow() }
            .toList()

        val ptr = PublicTournamentReportDEO(
            TournamentDEO.fromTournament(input),
            gameReports,
            allTeams
        )
        CacheUtil.cachePublicTournamentReport(ptr)
        ptr
    }.await()

}


suspend fun PublicTournamentReportDEO.Companion.fromTournamentId(id: Long): PublicTournamentReportDEO {
    return CacheUtil.getCachedPublicTournamentReport(id)
        .getOrElse {
            suspendedTransactionAsync {
                val tournament = Tournament.findById(id)
                    ?: throw IllegalArgumentException("Tournament with id $id does not exist")
                fromTournament(tournament)
            }.await()
        }

}

suspend fun CompleteTournamentReportDEO.Companion.fromTournament(input: Tournament): CompleteTournamentReportDEO {
    return suspendedTransactionAsync {

        val gameReports = TournamentReports.innerJoin(GameReports).select{
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

        val tljp =TournamentReports.innerJoin(Pitches)
        val tljps =tljp.select{
            (TournamentReports.tournament eq input.id) and
                    (TournamentReports.isSubmitted eq true)
        }
        val allPitchReports = tljps.map {
            TournamentReport.wrapRow(it)
            val pitch = Pitch.wrapRow(it)
            PitchDEO.fromPitch(pitch)
        }

        val deo = CompleteTournamentReportDEO(
            TournamentDEO.fromTournament(input),
            gameReports,
            allTeams,
            allPitchReports
        )
        CacheUtil.cacheCompleteTournamentReport(deo)
        deo
    }.await()
}


suspend fun CompleteTournamentReportDEO.Companion.fromTournamentId(id: Long): CompleteTournamentReportDEO {
    CacheUtil.getCachedCompleteTournamentReport(id).onSuccess {
        return it
    }
    return suspendedTransactionAsync {
        val tournament = Tournament.findById(id)
        if (tournament == null) {
            throw IllegalArgumentException("Tournament with id $id does not exist")
        }
        fromTournament(tournament)
    }.await()
}


suspend fun DeleteCompleteTournamentDEO.delete():Result<Long> {
    val tournamentID = this.id
    CacheUtil.deleteCachedPublicTournamentReport(tournamentID)
    CacheUtil.deleteCachedCompleteTournamentReport(tournamentID)
    return suspendedTransactionAsync {
        addLogger(StdOutSqlLogger)
        val tournament = Tournament.findById(tournamentID)
        if(tournament != null) {
            println("About to delete $tournament")
            val tournamentReports = TournamentReport.find { TournamentReports.tournament eq tournament.id }
            tournamentReports.forEach { tr ->
                tr.deleteComplete()
            }
            tournament.delete()
            Result.success(tournamentID)
        } else {
            Result.failure(Exception("Tournament with id $tournamentID not found"))
        }
    }.await()
}





