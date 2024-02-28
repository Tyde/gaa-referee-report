package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.lockedTransaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

suspend fun TournamentDEO.Companion.fromTournament(input: Tournament): TournamentDEO {

    return lockedTransaction {
        TournamentDEO(
            input.id.value, input.name, input.location, input.date, input.region.id.value
        )
    }
}

suspend fun TournamentDEO.updateInDatabase(): Result<Tournament> {
    val thisDEO = this
    return lockedTransaction {
        val region = Region.findById(thisDEO.region)
        if (region == null) {
            return@lockedTransaction Result.failure(
                IllegalArgumentException("Region with id ${thisDEO.region} does not exist")
            )
        }
        val tournament = Tournament.findById(thisDEO.id)
        if (tournament == null) {
            return@lockedTransaction Result.failure(
                IllegalArgumentException("Tournament with id ${thisDEO.id} does not exist")
            )
        }
        if (thisDEO.name.isBlank() || thisDEO.location.isBlank()) {
            return@lockedTransaction Result.failure(
                IllegalArgumentException("Name and location cannot be empty")
            )
        }
        runBlocking {
            CacheUtil.deleteCachedCompleteTournamentReport(tournament.id.value)
            CacheUtil.deleteCachedPublicTournamentReport(tournament.id.value)
        }

        TournamentReport.find { TournamentReports.tournament eq tournament.id }.forEach {
            runBlocking {
                CacheUtil.deleteCachedReport(it.id.value)
            }
        }

        tournament.name = thisDEO.name
        tournament.location = thisDEO.location
        tournament.date = thisDEO.date
        tournament.region = region
        return@lockedTransaction Result.success(tournament)
    }
}


suspend fun NewTournamentDEO.storeInDatabase(): Result<Tournament> {
    val thisDEO = this
    return lockedTransaction {
        val region = Region.findById(thisDEO.region)
        if (region == null) {
            return@lockedTransaction Result.failure(
                IllegalArgumentException("Region with id ${thisDEO.region} does not exist")
            )
        }
        if (name.isBlank() || location.isBlank()) {
            return@lockedTransaction Result.failure(
                IllegalArgumentException("Name and location cannot be empty")
            )
        }
        val tournament = Tournament.new {
            name = thisDEO.name
            location = thisDEO.location
            date = thisDEO.date
            this.region = region
        }
        return@lockedTransaction Result.success(tournament)
    }
}

fun RegionDEO.Companion.fromRegion(input: Region): RegionDEO {
    return RegionDEO(input.id.value, input.name)
}


suspend fun PublicTournamentReportDEO.Companion.fromTournament(input: Tournament): PublicTournamentReportDEO {
    return lockedTransaction {

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
        runBlocking {
            CacheUtil.cachePublicTournamentReport(ptr)
        }
        ptr
    }

}


fun PublicTournamentReportDEO.Companion.fromTournamentId(id: Long): PublicTournamentReportDEO {
    return runBlocking {
        CacheUtil.getCachedPublicTournamentReport(id)
            .getOrElse {
                suspendedTransactionAsync {
                    val tournament = Tournament.findById(id)
                        ?: throw IllegalArgumentException("Tournament with id $id does not exist")
                    fromTournament(tournament)
                }.await()
            }

    }
}

suspend fun CompleteTournamentReportDEO.Companion.fromTournament(input: Tournament): CompleteTournamentReportDEO {
    return lockedTransaction {

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
            .asFlow()
            .distinctUntilChanged()
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
        runBlocking {
            CacheUtil.cacheCompleteTournamentReport(deo)
        }
        deo
    }
}


fun CompleteTournamentReportDEO.Companion.fromTournamentId(id: Long): CompleteTournamentReportDEO {
    return runBlocking {
        CacheUtil.getCachedCompleteTournamentReport(id).onSuccess {
            return@runBlocking it
        }
        return@runBlocking lockedTransaction {
            val tournament = Tournament.findById(id)
            if (tournament == null) {
                throw IllegalArgumentException("Tournament with id $id does not exist")
            }
            fromTournament(tournament)
        }
    }
}


suspend fun DeleteCompleteTournamentDEO.delete():Result<Long> {
    val tournamentID = this.id
    runBlocking {
        CacheUtil.deleteCachedPublicTournamentReport(tournamentID)
        CacheUtil.deleteCachedCompleteTournamentReport(tournamentID)
    }
    return lockedTransaction {
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
    }
}





