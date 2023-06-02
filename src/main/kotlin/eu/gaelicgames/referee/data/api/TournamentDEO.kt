package eu.gaelicgames.referee.data.api

import eu.gaelicgames.referee.data.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate


@Serializable
data class TournamentDEO(
    val id: Long,
    val name: String,
    val location: String,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val region: Long
) {
    companion object {
        fun fromTournament(input: Tournament): TournamentDEO {
            return transaction{TournamentDEO(
                input.id.value, input.name, input.location, input.date, input.region.id.value
            )}
        }
    }
}


@Serializable
data class NewTournamentDEO(
    val name: String,
    val location: String,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val region: Long
) {
    fun storeInDatabase(): Result<Tournament> {
        val thisDEO = this
        return transaction {
            val region = Region.findById(thisDEO.region)
            if (region == null) {
                return@transaction Result.failure(
                    IllegalArgumentException("Region with id ${thisDEO.region} does not exist")
                )
            }
            val tournament = Tournament.new {
                name = thisDEO.name
                location = thisDEO.location
                date = thisDEO.date
                this.region = region
            }
            return@transaction Result.success(tournament)
        }
    }
}


@Serializable
data class RegionDEO(
    val id: Long,
    val name: String
) {
    companion object {
        fun fromRegion(input: Region): RegionDEO {
            return RegionDEO(input.id.value, input.name)
        }
    }
}




