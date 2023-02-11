package eu.gaelicgames.referee.plugins.routing

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.data.api.*
import eu.gaelicgames.referee.resources.Api
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.adminApiRouting() {

    get<Api.Tournaments.All> {
        val tournaments = transaction {
            Tournament.all().sortedBy { it.date }.map { TournamentDEO.fromTournament(it) }
        }
        call.respond(tournaments)
    }

    get<Api.Reports.All> {
        val reports = CompactTournamentReportDEO.all()
        call.respond(reports)
    }

    post<Api.PitchProperty.New> {
        TODO()
    }

    post<Api.PitchProperty.Update> {
        val updated = call.runCatching {
            this.receive<PitchVariableUpdateDEO>()
        }
        if (updated.isSuccess) {
            val dBUpdated = updated.getOrThrow().updateInDatabase()
            if (dBUpdated.isSuccess) {
                call.respond(
                    dBUpdated.getOrThrow()
                )
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.INSERTION_FAILED,
                        dBUpdated.exceptionOrNull()?.message ?: "Unknown error"
                    )
                )
            }
        } else {
            println(updated.exceptionOrNull())
            call.respond(
                ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    "Not able to parse pitch property update"
                )
            )
        }
    }

    post<Api.PitchProperty.Delete> {
        TODO()
    }

    post<Api.Rule.New> {
        TODO()
    }

    post<Api.Rule.Update> {
        val updated = call.runCatching {
            this.receive<RuleDEO>()
        }
        if (updated.isSuccess) {
            val dbUpdated = updated.getOrThrow().updateInDatabase()
            if (dbUpdated.isSuccess) {
                call.respond(
                    RuleDEO.fromRule(dbUpdated.getOrThrow())
                )
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.INSERTION_FAILED,
                        dbUpdated.exceptionOrNull()?.message ?: "Unknown error"
                    )
                )
            }
        } else {
            call.respond(
                ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    "Not able to parse rule update: " + updated.exceptionOrNull()?.message
                )
            )
        }
    }

    post<Api.Rule.Disable> {
        toggleRuleState()

    }

    post<Api.Rule.Enable> {
        toggleRuleState()
    }

    post<Api.Rule.Delete> {
        val rule = call.runCatching {
            this.receive<ModifyRulesDEOState>()
        }
        if (rule.isSuccess) {
            val deleteResult = rule.getOrThrow().delete()
            if (deleteResult.isSuccess) {
                call.respond(
                    rule.getOrThrow()
                )
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.INSERTION_FAILED,
                        deleteResult.exceptionOrNull()?.message ?: "Unknown error"
                    )
                )
            }
        } else {
            call.respond(
                ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    "Not able to parse rule delete command: " + rule.exceptionOrNull()?.message
                )
            )
        }
    }

    post<Api.Rule.CheckDeletable> {
        val rule = call.runCatching {
            this.receive<ModifyRulesDEOState>()
        }
        if (rule.isSuccess) {
            val deleteResult = rule.getOrThrow().isDeletable()
            if (deleteResult.isSuccess) {
                call.respond(
                    deleteResult.getOrThrow()
                )
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.INSERTION_FAILED,
                        deleteResult.exceptionOrNull()?.message ?: "Unknown error"
                    )
                )
            }
        } else {
            call.respond(
                ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    "Not able to parse rule delete command: " + rule.exceptionOrNull()?.message
                )
            )
        }
    }

    get<Api.User.All> {
        val users = transaction {
            User.all().map { RefereeDEO.fromReferee(it) }
        }
        call.respond(users)
    }

    post<Api.User.Update> {
        val updated = call.runCatching {
            this.receive<RefereeDEO>()
        }
        if (updated.isSuccess) {
            val dBUpdated = updated.getOrThrow().updateInDatabase()
            val refereeDEO = transaction { RefereeDEO.fromReferee(dBUpdated.getOrThrow()) }
            if (dBUpdated.isSuccess) {
                call.respond(
                    refereeDEO
                )
            } else {
                call.respond(
                    ApiError(
                        ApiErrorOptions.INSERTION_FAILED,
                        dBUpdated.exceptionOrNull()?.message ?: "Unknown error"
                    )
                )
            }
        } else {
            call.respond(
                ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    "Not able to parse user update"
                )
            )
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.toggleRuleState() {
    val rule = call.runCatching {
        this.receive<ModifyRulesDEOState>()
    }
    if (rule.isSuccess) {
        val updateResult = rule.getOrThrow().toggleDisabledState()
        if (updateResult.isSuccess) {
            call.respond(
                RuleDEO.fromRule(updateResult.getOrThrow())
            )
        } else {
            call.respond(
                ApiError(
                    ApiErrorOptions.INSERTION_FAILED,
                    updateResult.exceptionOrNull()?.message ?: "Unknown error"
                )
            )
        }

    } else {
        call.respond(
            ApiError(
                ApiErrorOptions.INSERTION_FAILED,
                "Not able to parse rule dis-/enable: " + rule.exceptionOrNull()?.message
            )
        )
    }
}