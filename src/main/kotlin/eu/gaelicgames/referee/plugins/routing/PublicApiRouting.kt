package eu.gaelicgames.referee.plugins.routing

import arrow.core.getOrElse
import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.data.api.*
import eu.gaelicgames.referee.plugins.receiveAndHandleDEO
import eu.gaelicgames.referee.resources.Api
import eu.gaelicgames.referee.util.lockedTransaction
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.resources.post
import org.jetbrains.exposed.sql.selectAll
import java.io.ByteArrayOutputStream

fun Route.publicApiRouting() {
    get<Api.Rules> {
        call.respond(RuleDEO.allRules())
    }

    get<Api.GameReportVariables> {
        call.respond(GameReportClassesDEO.load())
    }

    get<Api.PitchVariables> {
        call.respond(PitchVariablesDEO.load())
    }
    get<Api.Codes> {
        call.respond(GameCodeDEO.allGameCodes())
    }

    get<Api.Tournaments.CompleteReportPublic> { completeReport ->
        val id = completeReport.id
        runCatching {
            PublicTournamentReportDEO.fromTournamentId(id).let {
                call.respond(it)
            }
        }.onFailure {
            call.respond(HttpStatusCode.NotFound)
            it.printStackTrace()
        }

    }

    get<Api.Regions.All> {
        val regions = lockedTransaction {
            Region.all().map { RegionDEO.fromRegion(it) }
        }
        call.respond(regions)
    }

    get<Api.Tournaments.All> {
        val tournaments = lockedTransaction {
            Tournaments.selectAll().sortedBy { Tournaments.date }.map {
                TournamentDEO.wrapRow(it)
            }
        }
        call.respond(tournaments)
    }

    get<Api.TeamsAvailable> {
        val teams = TeamDEO.allTeamList()
        call.respond(teams)
    }

    get<Api.WebsiteFeed> {
        val response = ClubAndCountyApi.get()
        call.respond(response)
    }

    post<Api.Teamsheet.Upload> {
        val multipartData = call.receiveMultipart()
        val stream = ByteArrayOutputStream()
        var fileName = ""
        var fileDescription = ""
        multipartData.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    fileDescription = part.value
                }

                is PartData.FileItem -> {
                    fileName = part.originalFileName as String
                    val fileBytes = part.streamProvider().readBytes()
                    stream.write(fileBytes)
                }

                else -> {

                }
            }
            part.dispose()
        }
        val byteArray = stream.toByteArray()
        val sizeInMb = byteArray.size / 1024.0 / 1024.0
        if (sizeInMb > 5) {
            call.respond(ApiError(ApiErrorOptions.ILLEGAL_ARGUMENT, "File too large"))
            return@post
        }

        call.respond(TeamsheetUploadSuccessDEO.fromBytes(byteArray).getOrElse {
           it.toApiResponse()
        })
    }

    post<Api.Teamsheet.SetMetadata> {
        receiveAndHandleDEO<TeamsheetWithClubAndTournamentDataDEO> { deo ->
            deo.storeInDatabase().map { deo }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.Teamsheet.GetPlayers> {
        receiveAndHandleDEO<TeamsheetFileKeyDEO> {
            it.getPlayers().getOrElse { fail ->
                fail.toApiResponse()
            }
        }
    }

    post<Api.Teamsheet.GetMetadata> {
        receiveAndHandleDEO<TeamsheetFileKeyDEO> {
            it.getMetadata().getOrElse {
                ApiError(ApiErrorOptions.ILLEGAL_ARGUMENT, it.message ?: "Unknown error")
            }
        }
    }

    get<Api.Teamsheet.GetPlayers> {
        call.respond("This endpoint only accepts POST requests")
    }
}
