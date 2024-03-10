package eu.gaelicgames.referee

import eu.gaelicgames.referee.data.User
import eu.gaelicgames.referee.plugins.configureRouting
import eu.gaelicgames.referee.plugins.configureSecurity
import eu.gaelicgames.referee.plugins.configureSerialization
import eu.gaelicgames.referee.plugins.configureTemplating
import eu.gaelicgames.referee.services.NotifyCCCService
import eu.gaelicgames.referee.util.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

suspend fun main() = runBlocking<Unit> {





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
    runBlocking {
        CacheUtil.init(GGERefereeConfig.redisHost+":"+GGERefereeConfig.redisPort, GGERefereeConfig.redisPassword)
    }

    DatabaseHandler.init()
    runBlocking {
        DatabaseHandler.createSchema()

        lockedTransaction {
            if (User.all().count() == 0L) {

                if (File("data/data.db").exists()) {
                    DatabaseHandler.migrateSQLiteToPostgres()
                }
            }
        }
        DatabaseHandler.populate_base_data()


        lockedTransaction {
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

    launch (Dispatchers.Default) {
        val notifyCCCService = NotifyCCCService(this)
        notifyCCCService.start()
    }




    configureTemplating()
    configureSerialization()
    configureSecurity()
    configureRouting()
    environment.monitor.subscribe(ApplicationStopping) {
        DatabaseHandler.pool.close()

        println("Application stopping...")
    }
}
