package eu.gaelicgames.referee

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import eu.gaelicgames.referee.plugins.configureRouting
import eu.gaelicgames.referee.plugins.configureSecurity
import eu.gaelicgames.referee.plugins.configureSerialization
import eu.gaelicgames.referee.plugins.configureTemplating

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureTemplating()
        configureRouting()
        configureSecurity()
        configureSerialization()
    }.start(wait = true)
}
