package eu.gaelicgames.referee.services

import eu.gaelicgames.referee.data.User
import eu.gaelicgames.referee.data.UserRole
import eu.gaelicgames.referee.data.Users
import eu.gaelicgames.referee.data.api.DisciplinaryActionStringDEO
import eu.gaelicgames.referee.data.api.getRedCardsIssuedOnThisDay
import eu.gaelicgames.referee.util.MailjetClientHandler
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.toKotlinDuration


class NotifyCCCService(
    scope: CoroutineScope,
    val targetTime: LocalTime = LocalTime.of(20, 0),
) : BaseRegularService(scope) {


    override suspend fun runCycle() {
        val cards = DisciplinaryActionStringDEO.getRedCardsIssuedOnThisDay()
        println("Checked cards for today:")
        cards.forEach { println(it) }
        if (cards.isNotEmpty()) {
            transaction {
                val cccMembers = User.find { Users.role eq UserRole.CCC }

                val mails = cccMembers.map {
                    val mail = "${it.firstName} ${it.lastName} <${it.mail}>"
                    val name = "${it.firstName} ${it.lastName}"
                    MailjetClientHandler.MailReceiverNamePair(name,mail)
                }
                println(mails)
                MailjetClientHandler.sendRedCardNotification(
                    cards,
                    mails
                )
            }
        }
    }


    suspend fun start() {
        val firstDelay = if (LocalTime.now().isAfter(this.targetTime)) {
            java.time.Duration.between(
                LocalDateTime.now(),
                this.targetTime
                    .atDate(
                        LocalDate.now()
                            .plusDays(1)
                    )
            ).toKotlinDuration()
        } else {
            java.time.Duration.between(
                LocalTime.now(),
                this.targetTime
            ).toKotlinDuration()
        }
        this.start((24).hours, firstDelay)
        //this.start((20).seconds, (20).seconds)
    }
}
