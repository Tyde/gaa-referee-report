package eu.gaelicgames.referee.data

import eu.gaelicgames.referee.data.GameCodeDEO
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync


fun GameCodeDEO(gameCode: GameCode): GameCodeDEO {
    return GameCodeDEO(gameCode.name, gameCode.id.value)
}

suspend fun GameCodeDEO.Companion.allGameCodes(): List<GameCodeDEO> {
    return suspendedTransactionAsync {
                val gameCodes = GameCode.all().map { GameCodeDEO(it) }
                gameCodes
            }.await()

}




