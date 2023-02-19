package eu.gaelicgames.referee.plugins.routing

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.data.api.*
import eu.gaelicgames.referee.plugins.receiveAndHandleDEO
import eu.gaelicgames.referee.resources.Api
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.adminApiRouting() {


    get<Api.Reports.All> {
        val reports = CompactTournamentReportDEO.all()
        call.respond(reports)
    }

    post<Api.PitchProperty.New> {
        receiveAndHandleDEO<NewPitchVariableDEO> { newDEO ->
            newDEO.createInDatabase().getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.PitchProperty.Update> {
        receiveAndHandleDEO<PitchVariableUpdateDEO> { deo->
            deo.updateInDatabase().getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.PitchProperty.Delete> {
        receiveAndHandleDEO<PitchVariableUpdateDEO> {
            it.delete().getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.PitchProperty.Enable>{
        receiveAndHandleDEO<PitchVariableUpdateDEO> {
            it.enable().getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.Rule.New> {
        receiveAndHandleDEO<NewRuleDEO> { newRuleDEO ->
            newRuleDEO.createInDatabase().map { RuleDEO.fromRule(it) }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.Rule.Update> {
        receiveAndHandleDEO<RuleDEO> { ruleDEO ->
            ruleDEO.updateInDatabase().map { RuleDEO.fromRule(it) }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.Rule.Disable> {
        toggleRuleState()
    }

    post<Api.Rule.Enable> {
        toggleRuleState()
    }

    post<Api.Rule.Delete> {
        receiveAndHandleDEO<ModifyRulesDEOState> { modifyRulesDEOState ->
            modifyRulesDEOState.delete().map { modifyRulesDEOState }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.Rule.CheckDeletable> {
        receiveAndHandleDEO<ModifyRulesDEOState> { modifyRulesDEOState ->
            modifyRulesDEOState.isDeletable().getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    get<Api.User.All> {
        val users = transaction {
            User.all().map { RefereeDEO.fromReferee(it) }
        }
        call.respond(users)
    }

    post<Api.User.Update> {
        receiveAndHandleDEO<RefereeDEO> { refereeDEO ->
            refereeDEO.updateInDatabase().map { transaction { RefereeDEO.fromReferee(it) }}.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.User.New> {
        receiveAndHandleDEO<NewRefereeDEO> { newRefereeDEO ->
            newRefereeDEO.createInDatabase().map { RefereeDEO.fromReferee(it) }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.Team.Update> {
        receiveAndHandleDEO<TeamDEO> { teamDEO ->
            teamDEO.updateInDatabase().map {
                TeamDEO.fromTeam(it)
            }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    post<Api.GameType.Update> {
        receiveAndHandleDEO<GameTypeDEO> { gameTypeDEO ->
            gameTypeDEO.updateInDatabase().map {
                GameTypeDEO.fromGameType(it)
            }.getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }
    /*
        get<Api.User.Activate> { activate ->
            val uuid = UUID.fromString(activate.uuid)
            val user = transaction {
                ActivationToken
                    .find { ActivationTokens.token eq uuid }
                    .firstOrNull()
                    ?.let {foundToken ->
                        User.find { Users.id eq foundToken.user.id }.firstOrNull()
                    }
            }


    }*/
}

private suspend fun PipelineContext<Unit, ApplicationCall>.toggleRuleState() {
    receiveAndHandleDEO<ModifyRulesDEOState> { deo ->
        deo.toggleDisabledState().map { RuleDEO.fromRule(it) }.getOrElse {
            ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
        }
    }
}