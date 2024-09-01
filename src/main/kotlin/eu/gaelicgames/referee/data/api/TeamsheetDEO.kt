package eu.gaelicgames.referee.data.api

import ExtractedPlayer
import TeamsheetReader
import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import aws.smithy.kotlin.runtime.content.toByteArray
import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.ObjectStorage
import eu.gaelicgames.referee.util.lockedTransaction
import java.time.LocalDateTime
import java.util.*


fun ExtractedPlayer.toPlayerDEO(): PlayerDEO {
    return PlayerDEO(this.romanName, null, this.number)
}

fun PlayerDEO.toRegisteredPlayer(): RegisteredPlayer {
    return RegisteredPlayer(this.name,this.jerseyNumber, this.playerNumber)
}

fun PlayerDEO.Companion.fromRegisteredPlayer(player: RegisteredPlayer): PlayerDEO {
    return PlayerDEO(player.name, player.jerseyNumber, player.foireannNumber)
}
suspend fun TeamsheetUploadSuccessDEO.Companion.fromBytes(
    data: ByteArray
): Either<TeamsheetFailure, TeamsheetUploadSuccessDEO> = either {
    val playerExtractionResult = TeamsheetReader.readFromBytes(data)
    val fileUUID = UUID.randomUUID()
    val upload = ObjectStorage.uploadObject(fileUUID.toString(), data)
    ensure(upload.isSuccess) {
        upload.exceptionOrNull()?.printStackTrace()
        TeamsheetFailure.TeamsheetStorageFailedDEO()
    }
    ensure(playerExtractionResult.isSuccess) { TeamsheetFailure.ExtractionFailedButUploadedDEO(fileUUID.toString()) }
    TeamsheetUploadSuccessDEO(playerExtractionResult.getOrThrow().map { it.toPlayerDEO() }, fileUUID.toString())
}



suspend fun TeamsheetWithClubAndTournamentDataDEO.storeInDatabase(): Result<TeamsheetRegistration> {
    val deo = this
    return lockedTransaction {
        val club = Team.findById(deo.clubId)
            ?: return@lockedTransaction Result.failure(IllegalArgumentException("Club with id ${deo.clubId} not found"))
        val tournament = Tournament.findById(deo.tournamentId) ?: return@lockedTransaction Result.failure(
            IllegalArgumentException("Tournament with id ${deo.tournamentId} not found")
        )
        val code = GameCode.findById(deo.codeId)
            ?: return@lockedTransaction Result.failure(IllegalArgumentException("Game code with id ${deo.codeId} not found"))

        val registration = TeamsheetRegistration.new {
            this.team = club
            this.tournament = tournament
            this.fileKey = deo.fileKey
            this.registrarMail = deo.registrarMail
            this.registrarName = deo.registrarName
            this.code = code
            this.players = deo.players.map { it.toRegisteredPlayer() }
            this.uploadedAt = LocalDateTime.now()
        }
        Result.success(registration)

    }
}


fun TeamsheetFailure.toApiResponse() : Any {
    return when (this) {
        is TeamsheetFailure.ExtractionFailedButUploadedDEO -> {
            this
        }

        is TeamsheetFailure.TeamsheetStorageFailedDEO -> {
            ApiError(ApiErrorOptions.INSERTION_FAILED, "Teamsheet storage failed")
        }
    }
}

suspend fun TeamsheetFileKeyDEO.getPlayers(): Either<TeamsheetFailure, TeamsheetUploadSuccessDEO> {
    val file = ObjectStorage.getObject(this.fileKey)
    return file.fold(
        onSuccess = {data ->
            val playerExtractionResult = TeamsheetReader.readFromBytes(data)
            if(!playerExtractionResult.isSuccess) { TeamsheetFailure.ExtractionFailedButUploadedDEO(this.fileKey).left() }
            TeamsheetUploadSuccessDEO(playerExtractionResult.getOrThrow().map { it.toPlayerDEO() }, this.fileKey).right()
        },
        onFailure = { TeamsheetFailure.TeamsheetStorageFailedDEO().left() }
    )
}

suspend fun TeamsheetFileKeyDEO.getMetadata(): Result<TeamsheetWithClubAndTournamentDataDEO> {
    val deo = this
    return lockedTransaction {
        val registration = TeamsheetRegistration.find { TeamsheetRegistrations.fileKey eq deo.fileKey }.firstOrNull()
            ?: return@lockedTransaction Result.failure(NoSuchElementException("No teamsheet with file key ${deo.fileKey} found"))
        Result.success(TeamsheetWithClubAndTournamentDataDEO(
            players = registration.players.map { PlayerDEO.fromRegisteredPlayer(it) },
            clubId = registration.team.id.value,
            tournamentId = registration.tournament.id.value,
            fileKey = registration.fileKey,
            registrarMail = registration.registrarMail,
            registrarName = registration.registrarName,
            codeId = registration.code.id.value
        ))
    }
}
