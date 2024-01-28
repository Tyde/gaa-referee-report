package eu.gaelicgames.referee.plugins.routing


import eu.gaelicgames.referee.data.api.*
import eu.gaelicgames.referee.resources.Api
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.CCCApiRouting() {

    get<Api.Tournaments.CompleteReport> { completeReport ->
        val id = completeReport.id
        runCatching {
            CompleteTournamentReportDEO.fromTournamentId(id).let {
                call.respond(it)
            }
        }.onFailure {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get<Api.Reports.All> {
        val reports = CompactTournamentReportDEO.all()
        call.respond(reports)
    }

}
