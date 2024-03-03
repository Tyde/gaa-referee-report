package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.lockedTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

suspend fun TournamentDEO.Companion.fromTournament(input: Tournament): TournamentDEO {

    return lockedTransaction {
        TournamentDEO(
            input.id.value, input.name, input.location, input.date, input.region.id.value
        )
    }
}

suspend fun TournamentDEO.Companion.wrapRow(row: ResultRow): TournamentDEO {

    val id = row[Tournaments.id].value
    val name = row[Tournaments.name]
    val location = row[Tournaments.location]
    val date = row[Tournaments.date]
    val region = row[Tournaments.region].value
    return TournamentDEO(id, name, location, date, region)

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

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun CompleteTournamentReportDEO.Companion.fromTournament(input: Tournament): CompleteTournamentReportDEO {
    return lockedTransaction {

        println("Before inner join")
        val gameReports = TournamentReports.innerJoin(GameReports).select {
            (TournamentReports.tournament eq input.id) and
                    (TournamentReports.isSubmitted eq true)
        }.map {
            val tr = TournamentReport.wrapRow(it)
            val rep = CompleteGameReportWithRefereeReportDEO.wrapRow(it, tr)
            rep
        }

        val allTeamIds = gameReports
            .flatMap { listOf(it.gameReport.gameReport.teamA, it.gameReport.gameReport.teamB) }
            .distinct()
            .filterNotNull()

        val (join,addedTeamAlias) = TeamDEO.wrapJoinQuery()
        val allTeams = join.selectAll().where { Teams.id inList allTeamIds }
            .map {
                TeamDEO.wrapJoinedRow(it, addedTeamAlias)
            }.toList().groupBy { it.id }.map {(id,tDEOList) ->
                val template =tDEOList.first()
                TeamDEO(
                    name = template.name,
                    id= template.id,
                    isAmalgamation = template.isAmalgamation,
                    amalgamationTeams = tDEOList.flatMap { it.amalgamationTeams?: listOf() }
                )

            }


        /*
        val allTeams = Teams.selectAll().where { Teams.id inList allTeamIds}
            .map { TeamDEO.fromTeamId(it) }
            .filter { it.isSuccess }
            .map { it.getOrThrow() }
            .toList()*/

        val tljp = TournamentReports.innerJoin(Pitches)
        val tljps = tljp.selectAll().where {
            (TournamentReports.tournament eq input.id) and
                    (TournamentReports.isSubmitted eq true)
        }
        val allPitchReports = tljps.map {
            PitchDEO.wrapRow(it)
        }

        val deo = CompleteTournamentReportDEO(
            TournamentDEO.fromTournament(input),
            gameReports.toList(),
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


suspend fun DeleteCompleteTournamentDEO.delete(): Result<Long> {
    val tournamentID = this.id
    runBlocking {
        CacheUtil.deleteCachedPublicTournamentReport(tournamentID)
        CacheUtil.deleteCachedCompleteTournamentReport(tournamentID)
    }
    return lockedTransaction {
        addLogger(StdOutSqlLogger)
        val tournament = Tournament.findById(tournamentID)
        if (tournament != null) {
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





