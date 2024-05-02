package eu.gaelicgames.referee.plugins.routing


import eu.gaelicgames.referee.data.ApiError
import eu.gaelicgames.referee.data.ApiErrorOptions
import eu.gaelicgames.referee.data.api.*
import eu.gaelicgames.referee.plugins.receiveAndHandleDEO
import eu.gaelicgames.referee.resources.Api
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.resources.post


fun Route.CCCApiRouting() {

    get<Api.Tournaments.CompleteReport> { completeReport ->
        println("Start of serving complete report")
        val id = completeReport.id
        runCatching {
            CompleteTournamentReportDEO.fromTournamentId(id).let {
                call.respond(it)
            }
        }.onFailure {
            it.printStackTrace()
            call.respond(HttpStatusCode.NotFound)
        }
        println("End of serving complete report")
    }

    get<Api.Reports.All> {
        val reports = CompactTournamentReportDEO.all()
        call.respond(reports)
    }

    post<Api.Tournaments.PreselectedTeams.AddTeams> {
        receiveAndHandleDEO<TournamentTeamPreselectionDEO> {  deo ->
            deo.add().getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Could not load preselected teams")
            }
        }
    }

    post<Api.Tournaments.PreselectedTeams.SetTeams> {
        receiveAndHandleDEO<TournamentTeamPreselectionDEO> { deo ->
            deo.set(true).getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Could not update preselected teams")
            }
        }
    }

    post<Api.Tournaments.PreselectedTeams.RemoveTeams> {
        receiveAndHandleDEO<TournamentTeamPreselectionDEO> { deo ->
            deo.deleteTeams().getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Could not delete preselected teams")
            }
        }
    }



}
