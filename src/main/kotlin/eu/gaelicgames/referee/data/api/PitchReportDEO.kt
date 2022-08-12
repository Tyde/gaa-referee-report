package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class PitchPropertyDEO(
    val id: Long,
    val name: String,
)

@Serializable
data class PitchVariablesDEO(
    val surfaces: List<PitchPropertyDEO>,
    val widths: List<PitchPropertyDEO>,
    val lengths: List<PitchPropertyDEO>,
    val markingsOptions: List<PitchPropertyDEO>,
    val goalPosts: List<PitchPropertyDEO>,
    val goalDimensions: List<PitchPropertyDEO>,
) {
    companion object {
        fun load(): PitchVariablesDEO {
            return transaction {
                PitchVariablesDEO(
                    surfaces = PitchSurfaceOption.all().map { it.toPitchPropertyDEO() },
                    lengths = PitchLengthOption.all().map { it.toPitchPropertyDEO() },
                    widths = PitchWidthOption.all().map { it.toPitchPropertyDEO() },
                    markingsOptions = PitchMarkingsOption.all().map { it.toPitchPropertyDEO() },
                    goalPosts = PitchGoalpostsOption.all().map { it.toPitchPropertyDEO() },
                    goalDimensions = PitchGoalDimensionOption.all().map { it.toPitchPropertyDEO() },
                )
            }
        }
    }
}

@Serializable
data class PitchReportDEO(
    val id: Long? = null,
    val name: String,
    val report: Long,
    val surface: Long? = null,
    val width: Long? = null,
    val length: Long? = null,
    val smallSquareMarkings: Long? = null,
    val penaltySquareMarkings: Long? = null,
    val thirteenMeterMarkings: Long? = null,
    val twentyMeterMarkings: Long? = null,
    val longMeterMarkings: Long? = null,
    val goalPosts: Long? = null,
    val goalDimensions: Long? = null,
    val additionalInformation: String? = null,
) {
    companion object {
        fun fromPitchReport(pitchReport: Pitch): PitchReportDEO {
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
    }

    fun createInDatabase(): Result<Pitch> {
        val pUpdate = this
        if (pUpdate.name.isNotBlank()) {
            return transaction {
                val report = TournamentReport.findById(pUpdate.report)
                if (report != null) {
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

    fun updateInDatabase(): Result<Pitch> {
        val pUpdate = this
        if (pUpdate.id != null && pUpdate.name.isNotBlank()) {
            return transaction {
                val report = TournamentReport.findById(pUpdate.report)
                if (report != null) {

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
                        return@transaction Result.success(pitch)
                    } else {
                        return@transaction Result.failure(IllegalArgumentException("Pitchreport not found"))
                    }

                } else {
                    return@transaction Result.failure(IllegalArgumentException("Report id not valid"))
                }
            }
        } else {
            return Result.failure(IllegalArgumentException("Pitch id not valid or name not set"))
        }
    }
}