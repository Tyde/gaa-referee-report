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

    var claudeAccessToken : String
    object mailjet : PropertyGroup() {
        val public by stringType
        val secret by stringType
    }

    object server : PropertyGroup() {
        val url by stringType
    }

    object admin : PropertyGroup() {
        val first_name by stringType
        val last_name by stringType
        val mail by stringType
        val password by stringType
    }

    object redis : PropertyGroup() {
        val host by stringType
        val port by intType
        val password by stringType
    }

    object postgres : PropertyGroup() {
        val host by stringType
        val port by intType
        val database by stringType
        val user by stringType
        val password by stringType
    }

    object claude : PropertyGroup() {
        val accessToken by stringType
    }

    init {
        val config = EnvironmentVariables() overriding
            ConfigurationProperties.fromOptionalFile(File("gge-referee.properties"))

        mailjetPublicKey = config[mailjet.public]
        mailjetSecretKey = config[mailjet.secret]
        serverUrl = config[server.url]

        adminFirstName = config[Key("admin.first_name", stringType)]
        adminLastName = config[Key("admin.last_name", stringType)]
        adminMail = config[admin.mail]
        adminPassword = config[admin.password]

        redisHost = config[redis.host]
        redisPort = config[redis.port]
        redisPassword = config[redis.password]

        postgresHost = config[postgres.host]
        postgresPort = config[postgres.port]
        postgresDatabase = config[postgres.database]
        postgresUser = config[postgres.user]
        postgresPassword = config[postgres.password]

        claudeAccessToken = config[claude.accessToken]


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
