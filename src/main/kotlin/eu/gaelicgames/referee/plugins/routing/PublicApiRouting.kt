package eu.gaelicgames.referee.plugins.routing

import eu.gaelicgames.referee.data.GameCode
import eu.gaelicgames.referee.data.GameCodeDEO
import eu.gaelicgames.referee.data.Rule
import eu.gaelicgames.referee.data.api.GameReportClassesDEO
import eu.gaelicgames.referee.data.api.PitchVariablesDEO
import eu.gaelicgames.referee.data.api.PublicTournamentReportDEO
import eu.gaelicgames.referee.data.api.RuleDEO
import eu.gaelicgames.referee.resources.Api
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.publicApiRouting() {
    get<Api.Rules> {
        val rules = transaction {
            Rule.all().map { RuleDEO.fromRule(it) }
        }
        call.respond(rules)
    }

    get<Api.GameReportVariables> {
        call.respond(GameReportClassesDEO.load())
    }

    get<Api.PitchVariables> {
        call.respond(PitchVariablesDEO.load())
    }
    get<Api.Codes> {
        val allCodes = transaction {
            GameCode.all().map { GameCodeDEO(it) }
        }
        call.respond(allCodes)
    }

    get<Api.Tournaments.CompleteReport> { completeReport ->
        val id = completeReport.id
        runCatching {
            PublicTournamentReportDEO.fromTournamentId(id).let {
                call.respond(it)
            }
        }.onFailure {
            call.respond(HttpStatusCode.NotFound)
        }

    }
}