package eu.gaelicgames.referee

import at.favre.lib.crypto.bcrypt.BCrypt
import eu.gaelicgames.referee.data.User
import eu.gaelicgames.referee.data.Users
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import eu.gaelicgames.referee.plugins.configureRouting
import eu.gaelicgames.referee.plugins.configureSecurity
import eu.gaelicgames.referee.plugins.configureSerialization
import eu.gaelicgames.referee.plugins.configureTemplating
import eu.gaelicgames.referee.util.DatabaseHandler
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {

    DatabaseHandler.init()
    DatabaseHandler.createSchema()
    DatabaseHandler.populate_base_data()

    transaction {
        if (User.all().count() == 0L) {
            println("No Admin user registered. Do that now:")
            println("First name:")
            val firstName = readLine()!!
            println("Last name:")
            val lastName = readLine()!!
            println("Mail:")
            val mail = readLine()!!
            println("Password:")
            val password = readLine()!!

            User.new {
                this.firstName = firstName
                this.lastName = lastName
                this.mail = mail
                this.password = BCrypt.withDefaults().hash(12, password.toCharArray())
            }
            println("Saved")
        }
    }



    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureTemplating()
        configureSerialization()
        configureRouting()
        configureSecurity()

    }.start(wait = true)
}
