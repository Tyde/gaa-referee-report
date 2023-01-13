package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import kotlinx.serialization.Serializable
import java.time.LocalDate


@Serializable
data class TournamentDEO(
    val id: Long,
    val name: String,
    val location: String,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate
) {
    companion object {
        fun fromTournament(input: Tournament): TournamentDEO {
            return TournamentDEO(
                input.id.value, input.name, input.location, input.date
            )
        }
    }
}


@Serializable
data class NewTournamentDEO(
    val name: String, val location: String, @Serializable(with = LocalDateSerializer::class) val date: LocalDate
)

