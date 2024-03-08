package eu.gaelicgames.referee.data

import eu.gaelicgames.referee.util.lockedTransaction


fun GameCodeDEO(gameCode: GameCode): GameCodeDEO {
    return GameCodeDEO(gameCode.name, gameCode.id.value)
}

suspend fun GameCodeDEO.Companion.allGameCodes(): List<GameCodeDEO> {
    return lockedTransaction {
                val gameCodes = GameCode.all().map { GameCodeDEO(it) }
                gameCodes
            }

}




