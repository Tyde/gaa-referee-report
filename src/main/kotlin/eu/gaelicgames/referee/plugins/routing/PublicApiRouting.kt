package eu.gaelicgames.referee.plugins.routing

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.data.api.*
import eu.gaelicgames.referee.resources.Api
import eu.gaelicgames.referee.util.lockedTransaction
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.selectAll

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
}
