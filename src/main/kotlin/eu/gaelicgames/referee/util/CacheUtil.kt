package eu.gaelicgames.referee.util

import eu.gaelicgames.referee.data.SessionWithUserData
import eu.gaelicgames.referee.data.api.*
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.github.crackthecodeabhi.kreds.connection.newClient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

import kotlinx.serialization.json.Json
import java.util.*


object CacheUtil {
    private lateinit var client:KredsClient
    /*init {
        client = newClient(
            Endpoint.from("127.0.0.1:6379")
        )
    }*/

    suspend fun init(address: String, password: String) {
        client = newClient(
            Endpoint.from(address)
        )
        if(password.isNotEmpty()) {
            client.auth(password)
        }
    }


    private const val KEY_REPORT = "report"

    private const val KEY_TOURNAMENT_REPORT = "tournamentReport"

    private const val KEY_PUBLIC_TOURNAMENT_REPORT = "publicTournamentReport"

    suspend fun getCachedReport(id:Long):Result<CompleteReportDEO> {
        client.run {
            val reportString = get("$KEY_REPORT:$id")
            if(reportString != null) {
                return kotlin.runCatching {
                    Json.decodeFromString<CompleteReportDEO>(reportString)
                }
            }
            return Result.failure(Exception("Report not found"))
        }
    }

    suspend fun cacheReport(report:CompleteReportDEO):Result<Unit> {
        client.run {
            return kotlin.runCatching {
                set("$KEY_REPORT:${report.id}", Json.encodeToString(report))
            }
        }
    }


    suspend fun getCachedCompleteTournamentReport(tournamentID:Long):Result<CompleteTournamentReportDEO> {
        client.run {
            val reportString = get("$KEY_TOURNAMENT_REPORT:$tournamentID")
            if(reportString != null) {
                return kotlin.runCatching {
                    Json.decodeFromString<CompleteTournamentReportDEO>(reportString)
                }
            }
            return Result.failure(Exception("Report not found"))
        }
    }

    suspend fun cacheCompleteTournamentReport(report:CompleteTournamentReportDEO):Result<Unit> {
        client.run {
            return kotlin.runCatching {
                set("$KEY_TOURNAMENT_REPORT:${report.tournament.id}", Json.encodeToString(report))
                expire("$KEY_TOURNAMENT_REPORT:${report.tournament.id}", (60*60*24*2).toULong())
            }
        }
    }

    suspend fun deleteCachedCompleteTournamentReport(tournamentID:Long):Result<Unit> {
        client.run {
            return kotlin.runCatching {
                del("$KEY_TOURNAMENT_REPORT:$tournamentID")
            }
        }
    }

    suspend fun cacheSession(session:SessionWithUserData):Result<Unit> {
        client.run {
            return kotlin.runCatching {
                jsonSet(
                    "session:${session.uuid}",
                    "$",
                    Json.encodeToString(session))
                expire("session:${session.uuid}", (60*60*24*2).toULong())
            }
        }
    }

    suspend fun getCachedSession(uuid: UUID):Result<SessionWithUserData> {
        client.run {
            val sessionString = jsonGet("session:$uuid", "$")
            if(sessionString != null) {
                return kotlin.runCatching {
                    Json.decodeFromString<List<SessionWithUserData>>(sessionString)[0]
                }
            }
            return Result.failure(Exception("Session not found"))
        }
    }

    suspend fun deleteCachedSession(uuid: UUID):Result<Unit> {
        client.run {
            return kotlin.runCatching {
                del("session:$uuid")
            }
        }
    }

    suspend fun cachePitchVariables(variables:PitchVariablesDEO):Result<Unit> {
        client.run {
            return kotlin.runCatching {
                jsonSet(
                    "pitchVariables",
                    "$",
                    Json.encodeToString(variables))
            }
        }
    }

    suspend fun getCachedPitchVariables():Result<PitchVariablesDEO> {
        client.run {
            val variablesString = jsonGet("pitchVariables", "$")
            if(variablesString != null) {
                return kotlin.runCatching {
                    Json.decodeFromString<List<PitchVariablesDEO>>(variablesString)[0]
                }
            }
            return Result.failure(Exception("Pitch variables not found"))
        }
    }

    suspend fun deleteCachedPitchVariables():Result<Unit> {
        client.run {
            return kotlin.runCatching {
                del("pitchVariables")
            }
        }
    }

    suspend fun cacheRules( rules:List<RuleDEO>):Result<Unit> {
        client.run {
            return kotlin.runCatching {
                jsonSet(
                    "rules",
                    "$",
                    Json.encodeToString(rules))
            }
        }
    }

    suspend fun getCachedRules():Result<List<RuleDEO>> {
        client.run {
            val rulesString = jsonGet("rules","$")
            if(rulesString != null) {
                return kotlin.runCatching {
                    Json.decodeFromString<List<List<RuleDEO>>>(rulesString)[0]
                }
            }
            return Result.failure(Exception("Rules not found"))
        }
    }

    suspend fun deleteCachedRules():Result<Unit> {
        client.run {
            return kotlin.runCatching {
                del("rules")
            }
        }
    }

    suspend fun cachePublicTournamentReport(report: PublicTournamentReportDEO):Result<Unit> {
        client.run {
            return kotlin.runCatching {
                set("$KEY_PUBLIC_TOURNAMENT_REPORT:${report.tournament.id}", Json.encodeToString(report))
                expire("$KEY_PUBLIC_TOURNAMENT_REPORT:${report.tournament.id}", (60*60*6).toULong())
            }
        }
    }

    suspend fun getCachedPublicTournamentReport(tournamentID:Long):Result<PublicTournamentReportDEO> {
        client.run {
            val reportString = get("$KEY_PUBLIC_TOURNAMENT_REPORT:$tournamentID")
            if(reportString != null) {
                return kotlin.runCatching {
                    Json.decodeFromString<PublicTournamentReportDEO>(reportString)
                }
            }
            return Result.failure(Exception("Report not found"))
        }
    }

    suspend fun deleteCachedPublicTournamentReport(tournamentID:Long):Result<Unit> {
        client.run {
            return kotlin.runCatching {
                del("$KEY_PUBLIC_TOURNAMENT_REPORT:$tournamentID")
            }
        }
    }
}
