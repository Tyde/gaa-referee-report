package eu.gaelicgames.referee.util

import com.mailjet.client.ClientOptions
import com.mailjet.client.MailjetClient
import com.mailjet.client.MailjetRequest
import com.mailjet.client.resource.Emailv31
import eu.gaelicgames.referee.data.api.DisciplinaryActionStringDEO
import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import io.ktor.server.application.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringWriter

object MailjetClientHandler {
    var mailjetClient: MailjetClient
    val freeMarkerConfig = Configuration(Configuration.VERSION_2_3_32)

    init {


        val options = ClientOptions.builder()
            .apiKey(GGERefereeConfig.mailjetPublicKey)
            .apiSecretKey(GGERefereeConfig.mailjetSecretKey)
            .build()
        val client = MailjetClient(options)
        this.mailjetClient = client
        val templateLoader = ClassTemplateLoader(Application::class.java.classLoader, "templates")
        //freeMarkerConfig.setDirectoryForTemplateLoading(File("templates/"))
        freeMarkerConfig.templateLoader = templateLoader



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
                .put(Emailv31.Message.TEMPLATEID, 4578064)
                .put(Emailv31.Message.TEMPLATELANGUAGE, true)
                .put(Emailv31.Message.SUBJECT, "You've been added as a Refere on the GGE Referee System")
                .put(
                    Emailv31.Message.VARIABLES,
                    JSONObject()
                        .put("confirmation_link", activationLink)
                        .put("referee_name", refereeName)
                )
        )
        //println(payload.toString())
        val request = MailjetRequest(Emailv31.resource).property(
            Emailv31.MESSAGES, payload
        )
        val response = mailjetClient.post(request)
        println(response.data)
    }

    fun sendPasswordResetMail(refereeName: String, resetLink: String, mail: String) {
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
                .put(Emailv31.Message.TEMPLATEID, 5634965)
                .put(Emailv31.Message.TEMPLATELANGUAGE, true)
                .put(Emailv31.Message.SUBJECT, "Your password on the GGE Referee Report system has been reset")
                .put(
                    Emailv31.Message.VARIABLES,
                    JSONObject()
                        .put("reset_link", resetLink)
                        .put("referee_name", refereeName)
                )
        )
        //println(payload.toString())
        val request = MailjetRequest(Emailv31.resource).property(
            Emailv31.MESSAGES, payload
        )
        val response = mailjetClient.post(request)
        println(response.data)
    }

    data class MailReceiverNamePair(val name:String,val mail:String)

    fun sendRedCardNotification(
        disciplinaryActions: List<DisciplinaryActionStringDEO>,
        receiver: List<MailReceiverNamePair>,
    ) {
        val redCardContent =mapOf("disciplinaryActions" to disciplinaryActions)
        val template = freeMarkerConfig.getTemplate("redCardMail.ftl")
        val stringWriter = StringWriter()
        template.process(redCardContent, stringWriter)


        val receiverJSONArray = JSONArray()
        receiver.map {
            JSONObject().put("Email", it.mail).put("Name",it.name)
        }.forEach {
            receiverJSONArray.put(it)
        }

        val payload = JSONArray().put(
            JSONObject().put(
                Emailv31.Message.FROM,
                JSONObject().put("Email", "refereereportsystem@gaelicgames.eu")
                    .put("Name", "GGE Referee Report System")
            ).put(
                Emailv31.Message.TO, receiverJSONArray
            )
                .put(Emailv31.Message.TEMPLATEID, 5584303)
                .put(Emailv31.Message.TEMPLATELANGUAGE, true)
                .put(Emailv31.Message.SUBJECT, "Red cards entered into referee system")
                .put(
                    Emailv31.Message.VARIABLES, JSONObject()
                        .put("redCardList", stringWriter.toString())
                        .put("review_link", "")
                )
        )
        val request = MailjetRequest(Emailv31.resource).property(
            Emailv31.MESSAGES, payload
        )
        val response = mailjetClient.post(request)
        println(response.data)
    }
}


