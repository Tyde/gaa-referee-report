package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.lockedTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.StatementType
import java.time.LocalDate

suspend fun TournamentDEO.Companion.fromTournament(input: Tournament): TournamentDEO {

    return lockedTransaction {
        TournamentDEO(
            input.id.value,
            input.name,
            input.location,
            input.date,
            input.region.id.value,
            input.isLeague,
            input.endDate
        )
    }
}

suspend fun TournamentDEO.Companion.wrapRow(row: ResultRow): TournamentDEO {

    val id = row[Tournaments.id].value
    val name = row[Tournaments.name]
    val location = row[Tournaments.location]
    val date = row[Tournaments.date]
    val region = row[Tournaments.region].value
    val isLeague = row[Tournaments.isLeague]
    val endDate = row[Tournaments.endDate]
    return TournamentDEO(id, name, location, date, region, isLeague, endDate)

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
        if (thisDEO.isLeague != null && thisDEO.isLeague) {
            if (thisDEO.endDate == null) {
                return@lockedTransaction Result.failure(
                    IllegalArgumentException("End date cannot be null for a league")
                )
            }
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
        tournament.isLeague = thisDEO.isLeague ?: false
        tournament.endDate = thisDEO.endDate
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
        if (thisDEO.isLeague != null && thisDEO.isLeague) {
            if (thisDEO.endDate == null) {
                return@lockedTransaction Result.failure(
                    IllegalArgumentException("End date cannot be null for a league")
                )
            }
        }
        val tournament = Tournament.new {
            name = thisDEO.name
            location = thisDEO.location
            date = thisDEO.date
            this.region = region
            isLeague = thisDEO.isLeague ?: false
            endDate = thisDEO.endDate
        }
        return@lockedTransaction Result.success(tournament)
    }
}

fun RegionDEO.Companion.fromRegion(input: Region): RegionDEO {
    return RegionDEO(input.id.value, input.name)
}


suspend fun PublicTournamentReportDEO.Companion.fromTournament(input: Tournament): PublicTournamentReportDEO {
    return lockedTransaction {


        val gameReports = GameReports.leftJoin(DisciplinaryActions).leftJoin(TournamentReports).selectAll()
            .where {
                (TournamentReports.tournament eq input.id) and
                        (TournamentReports.isSubmitted eq true)
            }
            .map { it ->
                Pair(it, PublicDisciplinaryActionDEO.wrapRow(it))
            }.groupBy { it.first[GameReports.id] }
            .map { (id, list) ->
                PublicGameReportDEO(
                    gameReport = GameReportDEO.wrapRow(list.first().first),
                    disciplinaryActions = list.mapNotNull { it.second },
                    code = list.first().first[TournamentReports.code].value
                )
            }


        val allTeams = getAllTeamsOfGameReports(gameReports.map { it.gameReport })

        val ptr = PublicTournamentReportDEO(
            TournamentDEO.fromTournament(input),
            gameReports,
            allTeams
        )
        CacheUtil.cachePublicTournamentReport(ptr)

        ptr
    }

}


suspend fun PublicTournamentReportDEO.Companion.fromTournamentId(id: Long): PublicTournamentReportDEO {
    return CacheUtil.getCachedPublicTournamentReport(id)
        .getOrElse {
            lockedTransaction {
                val tournament = Tournament.findById(id)
                    ?: throw IllegalArgumentException("Tournament with id $id does not exist")
                fromTournament(tournament)
            }
        }


}


data class PublicTournamentListWithTeamsRow(
    val tournamentId: Long,
    val tournamentName: String,
    val location: String,
    val date: LocalDate,
    val isLeague: Boolean,
    val endDate: LocalDate?,
    val teamId: Long,
    val teamName: String,
    val isAmalgamation: Boolean,
    val constituentTeamId: Long?,
    val constituentTeamName: String?,
    val region: Long
)

suspend fun PublicTournamentListDEO.Companion.all(): PublicTournamentListDEO? {
    return lockedTransaction {
        val result = exec(
            """
              WITH tournament_teams AS (
        -- Get teams from team_a position
        SELECT DISTINCT
            t.id AS tournament_id,
            t.name AS tournament_name,
            t.location,
            t.date,
            t.is_league,
            t.end_date,
            t.region,
            team_a.id AS team_id,
            team_a.name AS team_name,
            team_a.is_amalgamation
        FROM 
            tournaments t
            LEFT JOIN tournamentreports tr ON tr.tournament = t.id
            LEFT JOIN gamereports gr ON gr.report_id = tr.id
            LEFT JOIN teams team_a ON gr.team_a = team_a.id
        WHERE 
            team_a.id IS NOT NULL
    
        UNION
    
        -- Get teams from team_b position
        SELECT DISTINCT
            t.id AS tournament_id,
            t.name AS tournament_name,
            t.location,
            t.date,
            t.is_league,
            t.end_date,
            t.region,
            team_b.id AS team_id,
            team_b.name AS team_name,
            team_b.is_amalgamation
        FROM 
            tournaments t
            LEFT JOIN tournamentreports tr ON tr.tournament = t.id
            LEFT JOIN gamereports gr ON gr.report_id = tr.id
            LEFT JOIN teams team_b ON gr.team_b = team_b.id
        WHERE 
            team_b.id IS NOT NULL
    )
    SELECT 
        tt.tournament_id,
        tt.tournament_name,
        tt.location,
        tt.date,
        tt.is_league,
        tt.end_date,
        tt.team_id,
        tt.team_name,
        tt.is_amalgamation,
        tt.region,
        at.added_team AS constituent_team_id,
        added_team_details.name AS constituent_team_name
    FROM 
        tournament_teams tt
        LEFT JOIN amalgamations at ON tt.is_amalgamation AND tt.team_id = at.amalgamation
        LEFT JOIN teams added_team_details ON at.added_team = added_team_details.id
    ORDER BY 
        tt.date,
        tt.tournament_name,
        tt.team_name,
        constituent_team_name;""", explicitStatementType = StatementType.SELECT
        ) { rs ->
            val outList = mutableListOf<PublicTournamentListWithTeamsRow>()
                while (rs.next()) {
                    outList.add(
                        PublicTournamentListWithTeamsRow(
                            rs.getLong("tournament_id"),
                            rs.getString("tournament_name"),
                            rs.getString("location"),
                            rs.getDate("date").toLocalDate(),
                            rs.getBoolean("is_league"),
                            rs.getDate("end_date")?.toLocalDate(),
                            rs.getLong("team_id"),
                            rs.getString("team_name"),
                            rs.getBoolean("is_amalgamation"),
                            rs.getLong("constituent_team_id"),
                            rs.getString("constituent_team_name"),
                            rs.getLong("region")
                        )
                    )
                }
            outList.toList()
        }
        val transformed = result?.groupBy { it.tournamentId }?.mapValues { it.value.groupBy { it.teamId } }
            ?.mapValues { it ->
                val teams = it.value.mapValues { innerRow ->
                    val innerVal = innerRow.value
                    val amalgamatedTeams = if (innerVal.first().isAmalgamation) {
                        innerVal.map { teamRow ->
                            TeamDEO(
                                teamRow.constituentTeamName ?: "",
                                teamRow.constituentTeamId ?: -1,
                                false,
                                listOf()
                            )
                        }
                    } else {
                        listOf()
                    }
                    TeamDEO(
                        innerVal.first().teamName,
                        innerVal.first().teamId,
                        innerVal.first().isAmalgamation,
                        amalgamatedTeams
                    )
                }.values.toList()
                val firstRow = it.value.values.first().first()
                PublicTournamentWithTeamsDEO(
                    TournamentDEO(
                        firstRow.tournamentId,
                        firstRow.tournamentName,
                        firstRow.location,
                        firstRow.date,
                        firstRow.region,
                        firstRow.isLeague,
                        firstRow.endDate
                    ),
                    teams
                )
            }?.values?.toList()


        return@lockedTransaction transformed?.let { PublicTournamentListDEO(it) }

    }
}


@OptIn(ExperimentalCoroutinesApi::class)
suspend fun CompleteTournamentReportDEO.Companion.fromTournament(input: Tournament): CompleteTournamentReportDEO {
    return lockedTransaction {

        val gameReports = TournamentReports.innerJoin(GameReports).select {
            (TournamentReports.tournament eq input.id) and
                    (TournamentReports.isSubmitted eq true)
        }.map {
            val tr = TournamentReport.wrapRow(it)
            val rep = CompleteGameReportWithRefereeReportDEO.wrapRow(it, tr)
            rep
        }

        val allTeams = getAllTeamsOfGameReports(gameReports.map { it.gameReport.gameReport })


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
        CacheUtil.cacheCompleteTournamentReport(deo)

        deo
    }
}

private fun getAllTeamsOfGameReports(gameReports: List<GameReportDEO>): List<TeamDEO> {
    val allTeamIds = gameReports
        .flatMap { listOf(it.teamA, it.teamB) }
        .distinct()
        .filterNotNull()

    val (join, addedTeamAlias) = TeamDEO.wrapJoinQuery()
    val allTeams = TeamDEO.mapJoinedResultsToTeamDEO(
        join.selectAll().where { Teams.id inList allTeamIds }.toList(),
        addedTeamAlias
    )

    return allTeams
}


suspend fun CompleteTournamentReportDEO.Companion.fromTournamentId(id: Long): CompleteTournamentReportDEO {

    CacheUtil.getCachedCompleteTournamentReport(id).onSuccess {
        return it
    }
    return lockedTransaction {
        val tournament = Tournament.findById(id)
        if (tournament == null) {
            throw IllegalArgumentException("Tournament with id $id does not exist")
        }
        fromTournament(tournament)
    }

}


suspend fun DeleteCompleteTournamentDEO.delete(): Result<Long> {
    val tournamentID = this.id

    CacheUtil.deleteCachedPublicTournamentReport(tournamentID)
    CacheUtil.deleteCachedCompleteTournamentReport(tournamentID)

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

suspend fun MergeTournamentDEO.updateInDatabase(): Result<Tournament> {
    val mergeFromId = this.mergeFromId
    val mergeToId = this.mergeToId
    return lockedTransaction {
        val mergeFrom = Tournament.findById(mergeFromId)
        val mergeTo = Tournament.findById(mergeToId)
        if (mergeFrom == null || mergeTo == null) {
            return@lockedTransaction Result.failure(
                IllegalArgumentException("Tournament with id $mergeFromId or $mergeToId does not exist")
            )
        } else {
            val mergeFromReports = TournamentReport.find { TournamentReports.tournament eq mergeFrom.id }
            mergeFromReports.forEach {
                it.tournament = mergeTo
            }
            mergeFrom.delete()

            return@lockedTransaction Result.success(mergeTo)
        }
    }
}


