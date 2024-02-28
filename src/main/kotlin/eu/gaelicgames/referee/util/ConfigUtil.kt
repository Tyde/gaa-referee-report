package eu.gaelicgames.referee.util

import com.natpryce.konfig.*
import eu.gaelicgames.referee.data.User
import eu.gaelicgames.referee.data.UserRole
import java.io.File

object GGERefereeConfig {
    var mailjetSecretKey: String
    var mailjetPublicKey: String

    var serverUrl: String

    private var adminFirstName : String
    private var adminLastName : String
    private var adminMail : String
    private var adminPassword : String

    var redisHost : String
    var redisPort : Int
    var redisPassword : String = ""

    var postgresHost : String
    var postgresPort : Int
    var postgresDatabase : String
    var postgresUser : String
    var postgresPassword : String


    init {
        val config = EnvironmentVariables() overriding
            ConfigurationProperties.fromOptionalFile(File("gge-referee.properties"))
        val configMailjetPublicKey = Key("mailjet.public", stringType)
        val configMailjetSecretKey = Key("mailjet.secret", stringType)
        val configBaseUrl = Key("server.url", stringType)

        val configAdminFirstName = Key("admin.first_name", stringType)
        val configAdminLastName = Key("admin.last_name", stringType)
        val configAdminMail = Key("admin.mail", stringType)
        val configAdminPassword = Key("admin.password", stringType)

        val configRedisHost = Key("redis.host", stringType)
        val configRedisPort = Key("redis.port", intType)
        val configRedisPassword = Key("redis.password", stringType)

        val configPostgresHost = Key("postgres.host", stringType)
        val configPostgresPort = Key("postgres.port", intType)
        val configPostgresDatabase = Key("postgres.database", stringType)
        val configPostgresUser = Key("postgres.user", stringType)
        val configPostgresPassword = Key("postgres.password", stringType)

        mailjetPublicKey = config[configMailjetPublicKey]
        mailjetSecretKey = config[configMailjetSecretKey]
        serverUrl = config[configBaseUrl]

        adminFirstName = config[configAdminFirstName]
        adminLastName = config[configAdminLastName]
        adminMail = config[configAdminMail]
        adminPassword = config[configAdminPassword]

        redisHost = config[configRedisHost]
        redisPort = config[configRedisPort]
        redisPassword = config[configRedisPassword]

        postgresHost = config[configPostgresHost]
        postgresPort = config[configPostgresPort]
        postgresDatabase = config[configPostgresDatabase]
        postgresUser = config[configPostgresUser]
        postgresPassword = config[configPostgresPassword]


    }

    fun debug() {
        println(
            "Config loaded: " +
                    "mailjetPublicKey: $mailjetPublicKey, " +
                    "mailjetSecretKey: $mailjetSecretKey, " +
                    "serverUrl: $serverUrl, " +
                    "adminFirstName: $adminFirstName, " +
                    "adminLastName: $adminLastName, " +
                    "adminMail: $adminMail, " +
                    "adminPassword: $adminPassword"
        )
    }

    fun createAdminUser() {
        if (adminFirstName =="" || adminLastName =="" || adminMail =="" || adminPassword =="") {
            println("Admin user not configured. Please set the following properties in gge-referee.properties:")
            println("admin.first_name")
            println("admin.last_name")
            println("admin.mail")
            println("admin.password")
            throw Exception("Admin user not configured")
        }
        User.newWithPassword(
            adminFirstName,
            adminLastName,
            adminMail,
            adminPassword,
            UserRole.ADMIN
        )
    }
}
