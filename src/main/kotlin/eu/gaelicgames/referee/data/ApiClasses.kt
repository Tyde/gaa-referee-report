package eu.gaelicgames.referee.data

import eu.gaelicgames.referee.data.GameCodeDEO


fun GameCodeDEO(gameCode: GameCode): GameCodeDEO {
    return GameCodeDEO(gameCode.name, gameCode.id.value)
}




