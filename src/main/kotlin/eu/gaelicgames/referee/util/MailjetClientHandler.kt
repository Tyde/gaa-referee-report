package eu.gaelicgames.referee.util

import com.mailjet.client.ClientOptions
import com.mailjet.client.MailjetClient
import com.mailjet.client.MailjetRequest
import com.mailjet.client.resource.Emailv31
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import java.io.File

object MailjetClientHandler {
    lateinit var mailjetClient: MailjetClient
    init {


            val options = ClientOptions.builder()
                .apiKey(GGERefereeConfig.mailjetPublicKey)
                .apiSecretKey(GGERefereeConfig.mailjetSecretKey)
                .build()
            val client = com.mailjet.client.MailjetClient(options)
            this.mailjetClient = client
        }




    fun sendActivationMail(refereeName: String, activationLink: String, mail: String) {
        val payload = JSONArray().put(
            JSONObject().put(
                Emailv31.Message.FROM,
                JSONObject().put("Email", "refereereportsystem@gaelicgames.eu")
                    .put("Name", "GGE Referee Report System")
            ).put(
                Emailv31.Message.TO, JSONArray().put(
                    JSONObject().put("Email", mail).put("Name", refereeName)
                )
            )
                .put(Emailv31.Message.TEMPLATEID,4578064)
                .put(Emailv31.Message.TEMPLATELANGUAGE,true)
                .put(Emailv31.Message.SUBJECT,"You've been added as a Refere on the GGE Referee System")
                .put(Emailv31.Message.VARIABLES,
                    JSONObject()
                        .put("confirmation_link",activationLink)
                        .put("referee_name",refereeName)
                )
        )
        //println(payload.toString())
        val request = MailjetRequest(Emailv31.resource).property(
                Emailv31.MESSAGES,payload)
        val response = mailjetClient.post(request)
        println(response.data)
    }
}
