package eu.gaelicgames.referee.plugins.routing

import eu.gaelicgames.referee.data.ApiError
import eu.gaelicgames.referee.data.ApiErrorOptions
import eu.gaelicgames.referee.data.UserPrincipal
import eu.gaelicgames.referee.data.api.*
import eu.gaelicgames.referee.plugins.receiveAndHandleDEO
import eu.gaelicgames.referee.resources.Api
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminTokenManagementRouting() {

    post<Api.ApiToken.New> {
        val admin = call.principal<UserPrincipal>()?.user
        if (admin == null) {
            call.respond(ApiError(ApiErrorOptions.NOT_AUTHORIZED, "You are not authorized to call this api"))
            return@post
        }
        receiveAndHandleDEO<NewApiTokenDEO> { newDEO ->
            newDEO.createInDatabase(admin).getOrElse {
                ApiError(ApiErrorOptions.INSERTION_FAILED, it.message ?: "Unknown error")
            }
        }
    }

    get<Api.ApiToken.All> {
        call.respond(apiTokenList())
    }

    post<Api.ApiToken.Revoke> {
        receiveAndHandleDEO<RevokeApiTokenDEO> { revokeDEO ->
            revokeDEO.revoke().getOrElse {
                ApiError(ApiErrorOptions.DELETE_FAILED, it.message ?: "Unknown error")
            }
        }
    }
}
