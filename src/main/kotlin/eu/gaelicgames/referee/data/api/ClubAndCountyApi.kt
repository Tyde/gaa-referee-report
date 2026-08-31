package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.GameCodes
import eu.gaelicgames.referee.data.GameReports
import eu.gaelicgames.referee.data.GameType
import eu.gaelicgames.referee.data.GameTypes
import eu.gaelicgames.referee.util.CacheUtil
import eu.gaelicgames.referee.util.lockedTransaction
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter


suspend fun ClubAndCountyApi.Companion.get():ClubAndCountyApi {
    // Try to get from cache first
    val cachedResult = CacheUtil.getCachedClubAndCountyApi()
    if (cachedResult.isSuccess) {
        return cachedResult.getOrThrow()
    }
    
    // If not in cache or cache failed, query the database
    val result = lockedTransaction {
        val gameTypes = findAllFinalGameTypeIds()
        val query = """
            SELECT 
                gc.name as game_code_name,
                gr.start_time as start_time,
                gr.id as id,
                gr.team_a_points,
                gr.team_a_goals,
                gr.team_b_goals,
                gr.team_b_points,
                tou.name as tournament_name,
                tou.id as tournament_id,
                tou.is_league as is_league,
                tou.name as tournament_name,
                tou.location,
                tA.name as team_a_name,
                tA.id as team_a_id,
                tB.name as team_b_name,
                tB.id as team_b_id,
                u.last_name,
                u.first_name,
                gt.name as game_type_name
            FROM gamereports gr
                     INNER JOIN gametypes gt ON gr.game_type = gt.id
                JOIN teams tA ON tA.id = gr.team_a
                JOIN teams tB ON tB.id = gr.team_b
                JOIN tournamentreports tr ON tr.id = gr.report_id
                JOIN tournaments tou ON tou.id = tr.tournament
                JOIN gamecodes gc ON gc.id = tr.code
                JOIN users u ON u.id = tr.referee
            WHERE gt.name LIKE '% Final' OR tou.is_league = true
        """
        exec(query) {
            val resultList = mutableListOf<Pair<LocalDate,ClubAndCountyResultDEO>>()
            while (it.next()) {

                val codeName = it.getString("game_code_name")
                val timestamp = it.getTimestamp("start_time")
                val deo = ClubAndCountyResultDEO(
                    gender = simpleCodeToGenderConversion(codeName),
                    type = "results",
                    clubOrCounty = "club",
                    uniqueId = it.getLong("id").toString(),
                    date = timestamp.toLocalDateTime().toLocalDate(),
                    date_range = "",
                    time = timestamp.toLocalDateTime().toLocalTime(),
                    timestamp = timestamp.time/1000,
                    competitionName = it.getString("tournament_name"),
                    competitionShortName = "",
                    competitionId = it.getLong("tournament_id").toString(),
                    competitionStyle = translateCodeToCompetitionStyle(codeName),
                    competitionLevel = "",
                    competitionType = if (it.getBoolean("is_league")) "league" else "tournament",
                    roundName = it.getString("game_type_name"),
                    club1Name = it.getString("team_a_name"),
                    club1Id = it.getLong("team_a_id").toString(),
                    club2Name = it.getString("team_b_name"),
                    club2Id = it.getLong("team_b_id").toString(),
                    team1Goals = it.getString("team_a_goals"),
                    team1Points = it.getString("team_a_points"),
                    team2Goals = it.getString("team_b_goals"),
                    team2Points = it.getString("team_b_points"),
                    venueName = it.getString("location"),
                    venueId = "",
                    refereeSurname = it.getString("last_name"),
                    refereeForename = it.getString("first_name"),
                    refereeCounty = "",
                    result = true,
                    postponed = false,
                    replay = false,
                    sponsor = "",
                    parentCompetitionName = "GGE Championships",
                    team1Conceded = false,
                    team2Conceded = false,
                    abandoned = false,
                    neverPlayed = false,
                    owner = ""
                )
                resultList.add(Pair(timestamp.toLocalDateTime().toLocalDate(),deo))
            }
            val results = resultList
                .sortedByDescending { it.first }
                .groupBy { it.first }
                .map {
                    val valueOnly =it.value.map { it -> it.second }
                    DateTimeFormatter.ISO_DATE.format(it.key) to valueOnly
                 }.
                toMap()
            ClubAndCountyApi(results, listOf())

        }?: ClubAndCountyApi(mapOf(), listOf())
    }
    
    // Cache the result
    CacheUtil.cacheClubAndCountyApi(result)
    
    return result
}


fun simpleCodeToGenderConversion(codeName:String):String {
    when(codeName) {
        "Hurling" -> return "mens"
        "Camogie" -> return "womens"
        "Mens Football" -> return "mens"
        "Ladies Football" -> return "womens"
        else -> return ""
    }
}

fun translateCodeToCompetitionStyle(codeName: String):String {
    when(codeName) {
        "Hurling" -> return "hurling"
        "Camogie" -> return "camogie"
        "Mens Football" -> return "football"
        "Ladies Football" -> return "ladies_football"
        else -> return ""
    }
}
suspend fun findAllFinalGameTypeIds(): List<Long> {
    return lockedTransaction {
        GameType.find { GameTypes.name.like("%Final") }.map { it.id.value }.toList()
    }

}
