package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import io.netty.util.concurrent.Promise
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

@Serializable
data class CompleteReportDEO(
    val id: Long,
    val tournament: TournamentDEO,
    val code: Long,
    val additionalInformation: String,
    val isSubmitted: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class) val submitDate: LocalDateTime?,
    val selectedTeams: List<TeamDEO>,
    val gameReports: List<CompleteGameReportDEO>,
    val pitches: List<PitchDEO>,
    val referee: RefereeDEO,
) {
    companion object {
        fun fromTournamentReport(tournamentReport: TournamentReport): CompleteReportDEO {
            return transaction {
                CompleteReportDEO(
                    id = tournamentReport.id.value,
                    tournament = TournamentDEO.fromTournament(tournamentReport.tournament),
                    code = tournamentReport.code.id.value,
                    additionalInformation = tournamentReport.additionalInformation,
                    isSubmitted = tournamentReport.isSubmitted,
                    submitDate = tournamentReport.submitDate,
                    selectedTeams = tournamentReport.selectedTeams.map {
                        TeamDEO.fromTeam(it)
                    },
                    gameReports = tournamentReport.gameReports.map {
                        CompleteGameReportDEO.fromGameReport(it)
                    },
                    pitches = tournamentReport.pitches.map {
                        PitchDEO.fromPitch(it)
                    },
                    referee = RefereeDEO.fromReferee(tournamentReport.referee)
                )
            }
        }
    }
}

@Serializable
data class RefereeDEO(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val mail: String,
) {
    companion object {
        fun fromReferee(referee: User): RefereeDEO {
            return RefereeDEO(
                id = referee.id.value,
                firstName = referee.firstName,
                lastName = referee.lastName,
                mail = referee.mail,
            )
        }
    }
}

@Serializable
data class CompleteGameReportDEO(
    val gameReport: GameReportDEO,
    val injuries: List<InjuryDEO>,
    val disciplinaryActions: List<DisciplinaryActionDEO>,
) {
    companion object {
        fun fromGameReport(gameReport: GameReport): CompleteGameReportDEO {
            return transaction {
                CompleteGameReportDEO(
                    gameReport = GameReportDEO.fromGameReport(gameReport),
                    injuries = gameReport.injuries.map { InjuryDEO.fromInjury(it) },
                    disciplinaryActions = gameReport.disciplinaryActions.map {
                        DisciplinaryActionDEO.fromDisciplinaryAction(
                            it
                        )
                    })
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

@Serializable
data class UpdateReportAdditionalInformationDEO(
    val id: Long,
    val additionalInformation: String
) {
    companion object {
        fun fromTournamentReportReport(report: TournamentReport): UpdateReportAdditionalInformationDEO {
            return transaction {
                UpdateReportAdditionalInformationDEO(
                    id = report.id.value,
                    additionalInformation = report.additionalInformation
                )
            }
        }
    }

    fun updateInDatabase(): Result<TournamentReport> {
        val update = this
        if(update.id != null && update.additionalInformation.isNotBlank()) {
            return transaction {
                val report = TournamentReport.findById(update.id)
                if (report != null) {
                    report.additionalInformation = update.additionalInformation
                    Result.success(report)
                } else {
                    Result.failure(
                        IllegalArgumentException("Report with id ${update.id} not found")
                    )
                }
            }
        } else {
            return Result.failure(
                IllegalArgumentException("Id and additional information must be set")
            )
        }
    }
}

