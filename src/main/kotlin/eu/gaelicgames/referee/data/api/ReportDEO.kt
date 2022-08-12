package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
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
    val pitches: List<PitchDEO>
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
                    }
                )
            }
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