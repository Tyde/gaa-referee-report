package eu.gaelicgames.referee

import io.ktor.server.application.Application
import at.favre.lib.crypto.bcrypt.BCrypt
import com.mailjet.client.ClientOptions
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import com.typesafe.config.ConfigUtil
import eu.gaelicgames.referee.data.DisciplinaryAction
import eu.gaelicgames.referee.data.DisciplinaryActions
import eu.gaelicgames.referee.data.User
import eu.gaelicgames.referee.data.UserRole
import eu.gaelicgames.referee.data.api.DisciplinaryActionStringDEO
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import eu.gaelicgames.referee.plugins.configureRouting
import eu.gaelicgames.referee.plugins.configureSecurity
import eu.gaelicgames.referee.plugins.configureSerialization
import eu.gaelicgames.referee.plugins.configureTemplating
import eu.gaelicgames.referee.util.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.random.Random

suspend fun main() {

    DatabaseHandler.init()
    DatabaseHandler.createSchema()
    DatabaseHandler.populate_base_data()


    transaction {
        if (User.all().count() == 0L) {
            /*
            println("No Admin user registered. Do that now:")
            println("First name:")
            val firstName = readLine()!!
            println("Last name:")
            val lastName = readLine()!!
            println("Mail:")
            val mail = readLine()!!
            println("Password:")
            val password = readLine()!!*/

            GGERefereeConfig.createAdminUser()
        }
    }


    System.getenv("ADD_MOCK_DATA")?.let {
        println("Adding mock data")
        MockDataGenerator.addMockUsers()
        MockDataGenerator.addMockTournaments()
        MockDataGenerator.addMockTeams()
        for (i in 1..280) {
            MockDataGenerator.addMockTournamentReport()
        }
    }


    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        //watchPaths = listOf("gaa-referee-report", "classes", "resources"),
        watchPaths = listOf("gaa-referee-report", "classes", "resources"),
        module = Application::refereeApplicationModule
    ).start(wait = true)
}

fun Application.refereeApplicationModule() {
    runBlocking {  CacheUtil.init(GGERefereeConfig.redisHost+":"+GGERefereeConfig.redisPort, GGERefereeConfig.redisPassword)}

    configureTemplating()
    configureSerialization()
    configureSecurity()
    configureRouting()
}
