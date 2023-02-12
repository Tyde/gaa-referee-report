package eu.gaelicgames.referee.util

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import java.io.File

object GGERefereeConfig {
    lateinit var mailjetSecretKey: String
    lateinit var mailjetPublicKey: String

    lateinit var serverUrl: String

    init {
        val config = ConfigurationProperties.fromFile(File("gge-referee.properties"))
        val configMailjetPublicKey = Key("mailjet.public", stringType)
        val configMailjetSecretKey = Key("mailjet.secret", stringType)
        val configBaseUrl = Key("server.url", stringType)

        mailjetPublicKey = config[configMailjetPublicKey]
        mailjetSecretKey = config[configMailjetSecretKey]
        serverUrl = config[configBaseUrl]
    }
}