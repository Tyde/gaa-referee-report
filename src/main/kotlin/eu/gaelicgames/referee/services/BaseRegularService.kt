package eu.gaelicgames.referee.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

abstract class BaseRegularService(val scope: CoroutineScope) {
    var serviceIsActive = false
    abstract suspend fun runCycle()
    protected suspend fun start(runEvery: Duration, initialDelay: Duration?) {
        scope.launch {
            serviceIsActive = true
            if (initialDelay != null) {
                delay(100L)
                delay(initialDelay)
                runCycle()
            }
            while (serviceIsActive) {
                delay(runEvery)
                runCycle()
            }
        }

    }

}
