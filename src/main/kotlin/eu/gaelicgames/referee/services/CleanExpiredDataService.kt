package eu.gaelicgames.referee.services

import eu.gaelicgames.referee.data.ActivationTokens
import eu.gaelicgames.referee.data.Sessions
import eu.gaelicgames.referee.util.lockedTransaction
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class CleanExpiredDataService(
    scope: CoroutineScope,
) : BaseRegularService(scope) {
    private val logger = LoggerFactory.getLogger(CleanExpiredDataService::class.java)
    override suspend fun runCycle() {
        logger.info("Cleaning expired data")
        lockedTransaction {
            Sessions.deleteWhere {
                expires less LocalDateTime.now().minusDays(1)
            }
            ActivationTokens.deleteWhere {
                expires less LocalDateTime.now().minusDays(1)
            }
        }
        logger.info("Expired data cleaned")
    }

    suspend fun start() {
        this.start((24).hours, 1.minutes)
    }
}
