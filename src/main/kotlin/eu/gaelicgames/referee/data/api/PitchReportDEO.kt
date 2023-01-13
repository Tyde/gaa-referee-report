package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table
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

//PitchPropertyType Enum
enum class PitchPropertyType(val id: Long) {
    SURFACE(0),
    LENGTH(1),
    WIDTH(2),
    MARKINGS(3),
    GOALPOSTS(4),
    GOALDIMENSIONS(5);

    companion object {
        private val map = values().associateBy(PitchPropertyType::id)
        fun fromLong(longValue: Long): PitchPropertyType? {
            return map[longValue]
        }
    }

    fun toDBClass() :LongEntityClass<LongEntity> {
        return when (this) {
            SURFACE -> PitchSurfaceOption
            LENGTH -> PitchLengthOption
            WIDTH -> PitchWidthOption
            MARKINGS -> PitchMarkingsOption
            GOALPOSTS -> PitchGoalpostsOption
            GOALDIMENSIONS -> PitchGoalDimensionOption
        }

    }
}

//PitchPropertyType serializer by id
@Serializer(forClass = PitchPropertyType::class)
object PitchPropertyTypeSerializer : KSerializer<PitchPropertyType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PitchPropertyType", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): PitchPropertyType {
        val id = decoder.decodeLong()
        return PitchPropertyType.fromLong(id) ?: throw IllegalArgumentException("Unknown PitchPropertyType id: $id")
    }

    override fun serialize(encoder: Encoder, value: PitchPropertyType) {
        encoder.encodeLong(value.id)
    }
}

@Serializable
data class PitchVariableUpdateDEO(
    val id: Long,
    val name: String,
    @Serializable(with = PitchPropertyTypeSerializer::class) val type: PitchPropertyType
) {
    fun updateInDatabase(): Result<PitchVariableUpdateDEO> {
        var pvUpdate = this
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

@Serializable
data class DeletePitchReportDEO(
    val id:Long
) {
    fun deleteFromDatabase(): Result<Boolean> {
        val deleteId = this.id
        return transaction {
            val pitch = Pitch.findById(deleteId)
            if (pitch != null) {
                pitch.delete()
                Result.success(true)
            } else {
                Result.failure(IllegalArgumentException("Pitch not found"))
            }
        }
    }
}

@Serializable
data class PitchDEO(
    val id: Long?,
    val report: Long?,
    val name: String,
    val surface: Long? = null,
    val length: Long? = null,
    val width: Long? = null,
    val smallSquareMarkings: Long? = null,
    val penaltySquareMarkings: Long? = null,
    val thirteenMeterMarkings: Long? = null,
    val twentyMeterMarkings: Long? = null,
    val longMeterMarkings: Long? = null,
    val goalPosts: Long? = null,
    val goalDimensions: Long? = null,
    val additionalInformation: String
) {
    companion object {
        fun fromPitch(pitch: Pitch): PitchDEO {
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
    }
}