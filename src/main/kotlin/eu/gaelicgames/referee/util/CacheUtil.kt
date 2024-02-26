package eu.gaelicgames.referee.util

import eu.gaelicgames.referee.data.SessionWithUserData
import eu.gaelicgames.referee.data.api.*
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.github.crackthecodeabhi.kreds.connection.KredsClientConfig
import io.github.crackthecodeabhi.kreds.connection.newClient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*


object CacheUtil {
    private var client:KredsClient? = null
    private lateinit var password:String
    /*init {
        client = newClient(
            Endpoint.from("127.0.0.1:6379")
        )
    }*/

    suspend fun init(address: String, password: String) {
        val config = KredsClientConfig.Builder(
            connectTimeOutMillis = 2500
        ).build()
        println("Trying to connect to Cache")
        this.password = password
        try {
            client = newClient(
                Endpoint.from(address),
                config
            )
            println("Client initialized: $client")
            if(password.isNotEmpty()) {
                val res = client?.auth(password)
                println(res)
            }
            client?.clientInfo()
        } catch (e: Exception) {
            e.printStackTrace()
            client = null
        }

    }


    private const val KEY_REPORT = "report"

    private const val KEY_TOURNAMENT_REPORT = "tournamentReport"

    private const val KEY_PUBLIC_TOURNAMENT_REPORT = "publicTournamentReport"

    private const val KEY_TEAMS = "teams"

    private const val KEY_GAME_REPORT_CLASSES = "gameReportClasses"


    suspend fun <R> runClient(block: suspend KredsClient.() -> Result<R>):Result<R> {
        if(client != null) {
            if(password.isNotEmpty()) {
                client?.auth(password)
            }
            return client!!.block()
        } else {
            return Result.failure(Exception("Not connected to cache"))
        }
    }

    suspend fun getCachedReport(id:Long):Result<CompleteReportDEO> {
        return runClient {
            val reportString = get("$KEY_REPORT:$id")
            if(reportString != null) {
                return@runClient kotlin.runCatching {
                    Json.decodeFromString<CompleteReportDEO>(reportString)
                }
            }
            return@runClient Result.failure(Exception("Report not found"))
        }
    }

    suspend fun cacheReport(report:CompleteReportDEO): Result<Unit> {
        return runClient {
            kotlin.runCatching {
                set("$KEY_REPORT:${report.id}", Json.encodeToString(report))
                Unit
            }
        }

    }


    suspend fun deleteCachedReport(id:Long):Result<Unit> {
        return runClient {
            kotlin.runCatching {
                del("$KEY_REPORT:$id")
                Unit
            }
        }
    }

    suspend fun getCachedCompleteTournamentReport(tournamentID:Long):Result<CompleteTournamentReportDEO> {
        return runClient {
            val reportString = get("$KEY_TOURNAMENT_REPORT:$tournamentID")
            if(reportString != null) {
                return@runClient kotlin.runCatching {
                    Json.decodeFromString<CompleteTournamentReportDEO>(reportString)
                }
            }
            return@runClient Result.failure(Exception("Report not found"))
        }
    }

    suspend fun cacheCompleteTournamentReport(report:CompleteTournamentReportDEO):Result<Unit> {
        return runClient {
            kotlin.runCatching {
                set("$KEY_TOURNAMENT_REPORT:${report.tournament.id}", Json.encodeToString(report))
                expire("$KEY_TOURNAMENT_REPORT:${report.tournament.id}", (60*60*24*2).toULong())
                Unit
            }
        }
    }

    suspend fun deleteCachedCompleteTournamentReport(tournamentID:Long):Result<Unit> {
        return runClient  {
             kotlin.runCatching {
                del("$KEY_TOURNAMENT_REPORT:$tournamentID")
                Unit
            }
        }
    }

    suspend fun cacheSession(session:SessionWithUserData):Result<Unit> {
        return runClient  {
            kotlin.runCatching {
                jsonSet(
                    "session:${session.uuid}",
                    "$",
                    Json.encodeToString(session))
                expire("session:${session.uuid}", (60*60*24*2).toULong())
                Unit
            }
        }
    }

    suspend fun getCachedSession(uuid: UUID):Result<SessionWithUserData> {
        return runClient  {
            val sessionString = jsonGet("session:$uuid", "$")
            if(sessionString != null) {
                return@runClient kotlin.runCatching {
                    Json.decodeFromString<List<SessionWithUserData>>(sessionString)[0]
                }
            }
            return@runClient Result.failure(Exception("Session not found"))
        }
    }

    suspend fun deleteCachedSession(uuid: UUID):Result<Unit> {
        return runClient  {
            kotlin.runCatching {
                del("session:$uuid")
                Unit
            }
        }
    }

    suspend fun cachePitchVariables(variables:PitchVariablesDEO):Result<Unit> {
        return runClient  {
            kotlin.runCatching {
                jsonSet(
                    "pitchVariables",
                    "$",
                    Json.encodeToString(variables))
                Unit
            }
        }
    }

    suspend fun getCachedPitchVariables():Result<PitchVariablesDEO> {
        return runClient  {
            val variablesString = jsonGet("pitchVariables", "$")
            if(variablesString != null) {
                return@runClient kotlin.runCatching {
                    Json.decodeFromString<List<PitchVariablesDEO>>(variablesString)[0]
                }
            }
            return@runClient Result.failure(Exception("Pitch variables not found"))
        }
    }

    suspend fun deleteCachedPitchVariables():Result<Unit> {
        return runClient  {
            kotlin.runCatching {
                del("pitchVariables")
                Unit
            }
        }
    }

    suspend fun cacheRules( rules:List<RuleDEO>):Result<Unit> {
        return runClient  {
            kotlin.runCatching {
                jsonSet(
                    "rules",
                    "$",
                    Json.encodeToString(rules))
                return@runCatching
            }
        }
    }

    suspend fun getCachedRules():Result<List<RuleDEO>> {
        return runClient  {
            val rulesString = jsonGet("rules","$")
            if(rulesString != null) {
                return@runClient kotlin.runCatching {
                    Json.decodeFromString<List<List<RuleDEO>>>(rulesString)[0]
                }
            }
            return@runClient Result.failure(Exception("Rules not found"))
        }
    }

    suspend fun deleteCachedRules():Result<Unit> {
        return runClient  {
            kotlin.runCatching {
                del("rules")
                return@runCatching
            }
        }
    }

    suspend fun cachePublicTournamentReport(report: PublicTournamentReportDEO):Result<Unit> {
        return runClient  {
            kotlin.runCatching {
                set("$KEY_PUBLIC_TOURNAMENT_REPORT:${report.tournament.id}", Json.encodeToString(report))
                expire("$KEY_PUBLIC_TOURNAMENT_REPORT:${report.tournament.id}", (60*60*6).toULong())
                return@runCatching
            }
        }
    }

    suspend fun getCachedPublicTournamentReport(tournamentID:Long):Result<PublicTournamentReportDEO> {
        return runClient  {
            val reportString = get("$KEY_PUBLIC_TOURNAMENT_REPORT:$tournamentID")
            if(reportString != null) {
                return@runClient kotlin.runCatching {
                    Json.decodeFromString<PublicTournamentReportDEO>(reportString)
                }
            }
            return@runClient Result.failure(Exception("Report not found"))
        }
    }

    suspend fun deleteCachedPublicTournamentReport(tournamentID:Long):Result<Unit> {
        return runClient  {
            kotlin.runCatching {
                del("$KEY_PUBLIC_TOURNAMENT_REPORT:$tournamentID")
                return@runCatching
            }
        }
    }

    suspend fun cacheTeamList(teams:List<TeamDEO>):Result<Unit> {
        return runClient  {
            kotlin.runCatching {
                set(
                    KEY_TEAMS,
                    Json.encodeToString(teams))
                return@runCatching
            }
        }
    }

    suspend fun getCachedTeamList():Result<List<TeamDEO>> {
        return runClient  {
            val teamsString = get(KEY_TEAMS)
            if(teamsString != null) {
                return@runClient kotlin.runCatching {
                    Json.decodeFromString<List<TeamDEO>>(teamsString)
                }
            }
            return@runClient Result.failure(Exception("Teams not found"))
        }
    }

    suspend fun deleteCachedTeamList():Result<Unit> {
        return runClient  {
            kotlin.runCatching {
                del(KEY_TEAMS)
                return@runCatching
            }
        }
    }


    suspend fun GameReportClassesDEO.setCache():Result<Unit> {
        return runClient  {
            kotlin.runCatching {
                set(
                    KEY_GAME_REPORT_CLASSES,
                    Json.encodeToString(this@setCache)
                )
                return@runCatching
            }
        }
    }

    suspend fun GameReportClassesDEO.Companion.getCache():Result<GameReportClassesDEO> {
        return runClient  {
            val classesString = get(KEY_GAME_REPORT_CLASSES)
            if(classesString != null) {
                return@runClient kotlin.runCatching {
                    Json.decodeFromString<GameReportClassesDEO>(classesString)
                }
            }
            return@runClient Result.failure(Exception("Game report classes not found"))
        }
    }
    suspend fun GameReportClassesDEO.Companion.deleteCache():Result<Unit> {
        return runClient  {
            kotlin.runCatching {
                del(KEY_GAME_REPORT_CLASSES)
                return@runCatching
            }

        }
    }





}
