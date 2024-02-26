package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import eu.gaelicgames.referee.util.CacheUtil
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction


suspend fun PitchVariablesDEO.Companion.load(): PitchVariablesDEO {
    val cachedVars = CacheUtil.getCachedPitchVariables()
    if (cachedVars.isSuccess) {
        return cachedVars.getOrThrow()
    }

    return suspendedTransactionAsync {
        val variables = PitchVariablesDEO(
            surfaces = PitchSurfaceOption.all().map { it.toPitchPropertyDEO() },
            lengths = PitchLengthOption.all().map { it.toPitchPropertyDEO() },
            widths = PitchWidthOption.all().map { it.toPitchPropertyDEO() },
            markingsOptions = PitchMarkingsOption.all().map { it.toPitchPropertyDEO() },
            goalPosts = PitchGoalpostsOption.all().map { it.toPitchPropertyDEO() },
            goalDimensions = PitchGoalDimensionOption.all().map { it.toPitchPropertyDEO() },
        )
        CacheUtil.cachePitchVariables(variables)
        variables
    }.await()
}

fun PitchPropertyType.toDBClass(): LongEntityClass<LongEntity> {
    return when (this) {
        PitchPropertyType.SURFACE -> PitchSurfaceOption
        PitchPropertyType.LENGTH -> PitchLengthOption
        PitchPropertyType.WIDTH -> PitchWidthOption
        PitchPropertyType.MARKINGS -> PitchMarkingsOption
        PitchPropertyType.GOALPOSTS -> PitchGoalpostsOption
        PitchPropertyType.GOALDIMENSIONS -> PitchGoalDimensionOption
    }

}


suspend fun PitchVariableUpdateDEO.updateInDatabase(): Result<PitchVariableUpdateDEO> {
    val pvUpdate = this
    CacheUtil.deleteCachedPitchVariables()
    return transaction {
        val obj = pvUpdate.type.toDBClass()
            .findById(pvUpdate.id)
        if (obj is PitchPropertyEntity) {
            obj.name = pvUpdate.name
            Result.success(pvUpdate)
        } else {
            Result.failure(Exception("Could not find Pitch Property"))
        }
    }
}

suspend fun PitchVariableUpdateDEO.delete(): Result<DeletePitchVariableResultDEO> {
    val pvUpdate = this
    CacheUtil.deleteCachedPitchVariables()
    return transaction {
        val obj = pvUpdate.type.toDBClass()
            .findById(pvUpdate.id)
        if (obj is PitchPropertyEntity) {
            if (Pitches.isPitchPropertyReferenced(obj)) {
                obj.disabled = true
                Result.success(DeletePitchVariableResultDEO(pvUpdate.id, DeletionState.DISABLED))
            } else {
                obj.delete()
                Result.success(DeletePitchVariableResultDEO(pvUpdate.id, DeletionState.DELETED))
            }
        } else {
            Result.failure(Exception("Could not find Pitch Property to delete"))
        }
    }
}

suspend fun PitchVariableUpdateDEO.enable(): Result<PitchVariableUpdateDEO> {
    val pvUpdate = this
    CacheUtil.deleteCachedPitchVariables()
    return transaction {
        val obj = pvUpdate.type.toDBClass()
            .findById(pvUpdate.id)
        if (obj is PitchPropertyEntity) {
            obj.disabled = false
            Result.success(pvUpdate)
        } else {
            Result.failure(Exception("Could not find Pitch Property to enable"))
        }
    }
}


suspend fun NewPitchVariableDEO.createInDatabase(): Result<PitchVariableUpdateDEO> {
    val pvUpdate = this
    CacheUtil.deleteCachedPitchVariables()
    return transaction {
        val obj = when (pvUpdate.type) {
            PitchPropertyType.SURFACE -> PitchSurfaceOption.new { name = pvUpdate.name }
            PitchPropertyType.LENGTH -> PitchLengthOption.new { name = pvUpdate.name }
            PitchPropertyType.WIDTH -> PitchWidthOption.new { name = pvUpdate.name }
            PitchPropertyType.MARKINGS -> PitchMarkingsOption.new { name = pvUpdate.name }
            PitchPropertyType.GOALPOSTS -> PitchGoalpostsOption.new { name = pvUpdate.name }
            PitchPropertyType.GOALDIMENSIONS -> PitchGoalDimensionOption.new { name = pvUpdate.name }
        }
        Result.success(PitchVariableUpdateDEO(obj.id.value, obj.name, pvUpdate.type))
    }
}


fun PitchReportDEO.Companion.fromPitchReport(pitchReport: Pitch): PitchReportDEO {
    return transaction {
        PitchReportDEO(
            id = pitchReport.id.value,
            name = pitchReport.name,
            report = pitchReport.report.id.value,
            surface = pitchReport.surface?.id?.value,
            width = pitchReport.width?.id?.value,
            length = pitchReport.length?.id?.value,
            smallSquareMarkings = pitchReport.smallSquareMarkings?.id?.value,
            penaltySquareMarkings = pitchReport.penaltySquareMarkings?.id?.value,
            thirteenMeterMarkings = pitchReport.thirteenMeterMarkings?.id?.value,
            twentyMeterMarkings = pitchReport.twentyMeterMarkings?.id?.value,
            longMeterMarkings = pitchReport.longMeterMarkings?.id?.value,
            goalPosts = pitchReport.goalPosts?.id?.value,
            goalDimensions = pitchReport.goalDimensions?.id?.value,
            additionalInformation = pitchReport.additionalInformation,
        )
    }
}

suspend fun Transaction.clearCacheForPitchReport(pitch: Pitch) {

    val report = pitch.report
    clearCacheForTournamentReport(report)

}

suspend fun PitchReportDEO.createInDatabase(): Result<Pitch> {
    val pUpdate = this
    if (pUpdate.name.isNotBlank()) {
        return newSuspendedTransaction {
            val report = TournamentReport.findById(pUpdate.report)
            if (report != null) {
                clearCacheForTournamentReport(report)
                Result.success(Pitch.new {
                    this.name = pUpdate.name
                    this.report = report
                    pUpdate.surface?.let {
                        this.surface = PitchSurfaceOption.findById(it)
                    }
                    pUpdate.length?.let {
                        this.length = PitchLengthOption.findById(it)
                    }
                    pUpdate.width?.let {
                        this.width = PitchWidthOption.findById(it)
                    }
                    pUpdate.smallSquareMarkings?.let {
                        this.smallSquareMarkings = PitchMarkingsOption.findById(it)
                    }
                    pUpdate.penaltySquareMarkings?.let {
                        this.penaltySquareMarkings = PitchMarkingsOption.findById(it)
                    }
                    pUpdate.thirteenMeterMarkings?.let {
                        this.thirteenMeterMarkings = PitchMarkingsOption.findById(it)
                    }
                    pUpdate.twentyMeterMarkings?.let {
                        this.twentyMeterMarkings = PitchMarkingsOption.findById(it)
                    }
                    pUpdate.longMeterMarkings?.let {
                        this.longMeterMarkings = PitchMarkingsOption.findById(it)
                    }
                    pUpdate.goalPosts?.let {
                        this.goalPosts = PitchGoalpostsOption.findById(it)
                    }
                    pUpdate.goalDimensions?.let {
                        this.goalDimensions = PitchGoalDimensionOption.findById(it)
                    }
                    pUpdate.additionalInformation?.let {
                        this.additionalInformation = it
                    }
                })

            } else {
                Result.failure(IllegalArgumentException("Report not found"))
            }
        }
    } else {
        return Result.failure(IllegalArgumentException("Pitch name must be set"))
    }
}


suspend fun PitchReportDEO.updateInDatabase(): Result<Pitch> {
    val pUpdate = this
    if (pUpdate.id != null && pUpdate.name.isNotBlank()) {
        return newSuspendedTransaction {
            val report = TournamentReport.findById(pUpdate.report)
            if (report != null) {
                clearCacheForTournamentReport(report)
                val pitch = Pitch.findById(pUpdate.id)
                if (pitch != null) {
                    pitch.name = pUpdate.name
                    pitch.report = report
                    pUpdate.surface?.let {
                        pitch.surface = PitchSurfaceOption.findById(it)
                    }
                    pUpdate.length?.let {
                        pitch.length = PitchLengthOption.findById(it)
                    }
                    pUpdate.width?.let {
                        pitch.width = PitchWidthOption.findById(it)
                    }
                    pUpdate.smallSquareMarkings?.let {
                        pitch.smallSquareMarkings = PitchMarkingsOption.findById(it)
                    }
                    pUpdate.penaltySquareMarkings?.let {
                        pitch.penaltySquareMarkings = PitchMarkingsOption.findById(it)
                    }
                    pUpdate.thirteenMeterMarkings?.let {
                        pitch.thirteenMeterMarkings = PitchMarkingsOption.findById(it)
                    }
                    pUpdate.twentyMeterMarkings?.let {
                        pitch.twentyMeterMarkings = PitchMarkingsOption.findById(it)
                    }
                    pUpdate.longMeterMarkings?.let {
                        pitch.longMeterMarkings = PitchMarkingsOption.findById(it)
                    }
                    pUpdate.goalPosts?.let {
                        pitch.goalPosts = PitchGoalpostsOption.findById(it)
                    }
                    pUpdate.goalDimensions?.let {
                        pitch.goalDimensions = PitchGoalDimensionOption.findById(it)
                    }
                    pUpdate.additionalInformation?.let {
                        pitch.additionalInformation = it
                    }
                    return@newSuspendedTransaction Result.success(pitch)
                } else {
                    return@newSuspendedTransaction Result.failure(IllegalArgumentException("PitchReport not found"))
                }

            } else {
                return@newSuspendedTransaction Result.failure(IllegalArgumentException("Report id not valid"))
            }
        }
    } else {
        return Result.failure(IllegalArgumentException("Pitch id not valid or name not set"))
    }
}

suspend fun DeletePitchReportDEO.deleteChecked(user: User): Result<Boolean> {
    val deleteId = this.id
    return newSuspendedTransaction {
        Pitch.findById(deleteId)?.let {
            if (it.report.referee.id == user.id) {
                deleteFromDatabase()
            } else {
                Result.failure(IllegalArgumentException("No rights - User is not the author of this pitch report"))
            }
        } ?: Result.failure(IllegalArgumentException("Pitch report does not exist"))
    }
}


suspend fun DeletePitchReportDEO.deleteFromDatabase(): Result<Boolean> {
    val deleteId = this.id
    return newSuspendedTransaction {
        val pitch = Pitch.findById(deleteId)
        if (pitch != null) {
            clearCacheForPitchReport(pitch)
            pitch.delete()
            Result.success(true)
        } else {
            Result.failure(IllegalArgumentException("Pitch not found"))
        }
    }
}

fun PitchDEO.Companion.fromPitch(pitch: Pitch): PitchDEO {
    return transaction {
        PitchDEO(
            id = pitch.id.value,
            report = pitch.report.id.value,
            name = pitch.name,
            surface = pitch.surface?.id?.value,
            length = pitch.length?.id?.value,
            width = pitch.width?.id?.value,
            smallSquareMarkings = pitch.smallSquareMarkings?.id?.value,
            penaltySquareMarkings = pitch.penaltySquareMarkings?.id?.value,
            thirteenMeterMarkings = pitch.thirteenMeterMarkings?.id?.value,
            twentyMeterMarkings = pitch.twentyMeterMarkings?.id?.value,
            longMeterMarkings = pitch.longMeterMarkings?.id?.value,
            goalPosts = pitch.goalPosts?.id?.value,
            goalDimensions = pitch.goalDimensions?.id?.value,
            additionalInformation = pitch.additionalInformation
        )
    }
}


