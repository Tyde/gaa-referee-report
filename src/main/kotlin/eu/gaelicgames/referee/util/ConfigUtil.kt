package eu.gaelicgames.referee.util

import com.natpryce.konfig.*
import eu.gaelicgames.referee.data.User
import eu.gaelicgames.referee.data.UserRole
import java.io.File

object GGERefereeConfig {
    lateinit var mailjetSecretKey: String
    lateinit var mailjetPublicKey: String

    lateinit var serverUrl: String

    lateinit var adminFirstName : String
    lateinit var adminLastName : String
    lateinit var adminMail : String
    lateinit var adminPassword : String

    init {
        val config = EnvironmentVariables() overriding
            ConfigurationProperties.fromFile(File("gge-referee.properties"))
        val configMailjetPublicKey = Key("mailjet.public", stringType)
        val configMailjetSecretKey = Key("mailjet.secret", stringType)
        val configBaseUrl = Key("server.url", stringType)

        val configAdminFirstName = Key("admin.first_name", stringType)
        val configAdminLastName = Key("admin.last_name", stringType)
        val configAdminMail = Key("admin.mail", stringType)
        val configAdminPassword = Key("admin.password", stringType)

        mailjetPublicKey = config[configMailjetPublicKey]
        mailjetSecretKey = config[configMailjetSecretKey]
        serverUrl = config[configBaseUrl]

        adminFirstName = config[configAdminFirstName]
        adminLastName = config[configAdminLastName]
        adminMail = config[configAdminMail]
        adminPassword = config[configAdminPassword]
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