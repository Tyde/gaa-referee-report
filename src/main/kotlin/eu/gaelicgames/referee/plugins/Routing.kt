package eu.gaelicgames.referee.plugins

import eu.gaelicgames.referee.data.Amalgamation
import eu.gaelicgames.referee.data.Amalgamations
import eu.gaelicgames.referee.data.Team
import eu.gaelicgames.referee.data.TeamDEO
import eu.gaelicgames.referee.plugins.routing.adminApiRouting
import eu.gaelicgames.referee.plugins.routing.refereeApiRouting
import eu.gaelicgames.referee.plugins.routing.sites
import eu.gaelicgames.referee.resources.Api
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration


@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new") {
    @Serializable
    @Resource("new")
    class New(val parent: Articles = Articles())

    @Serializable
    @Resource("{id}")
    class Id(val parent: Articles = Articles(), val id: Long) {
        @Serializable
        @Resource("edit")
        class Edit(val parent: Id)
    }
}

fun Application.configureRouting() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    install(Resources)
    install(StatusPages) {
        exception<AuthenticationException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, _ ->
            call.respond(HttpStatusCode.Forbidden)
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondText(text = "404: Page Not Found", status = status)
            println(call.request.uri)
        }

    }
    routing {
        sites()

        get<Api.TeamsAvailable> {
            call.respond(transaction {
                Team.all().map {
                    if (!it.isAmalgamation) {
                        TeamDEO.fromTeam(it)
                    } else {
                        val addedTeams = Amalgamation.find { Amalgamations.amalgamation eq it.id }.map { amlgm ->
                            TeamDEO.fromTeam(amlgm.addedTeam)
                        }
                        TeamDEO.fromTeam(it, addedTeams)
                    }
                }
            })
        }

        authenticate("auth-session") {
            refereeApiRouting()
        }

        authenticate("admin-session") {
            adminApiRouting()
        }
        get("/*") {
            call.respondText("Catchall")
        }
    }

}


class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
