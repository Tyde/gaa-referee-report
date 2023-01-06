package eu.gaelicgames.referee.plugins.routing

import eu.gaelicgames.referee.data.ApiError
import eu.gaelicgames.referee.data.ApiErrorOptions
import eu.gaelicgames.referee.data.api.PitchPropertyDEO
import eu.gaelicgames.referee.data.api.PitchVariableUpdateDEO
import eu.gaelicgames.referee.resources.Api
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.ktor.server.resources.post
fun Route.adminApiRouting() {

    post<Api.PitchProperty.New> {

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

    }
}