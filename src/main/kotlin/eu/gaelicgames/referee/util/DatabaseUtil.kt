package eu.gaelicgames.referee.util

import eu.gaelicgames.referee.data.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.Month


object DatabaseHandler {
    var db: Database? = null
    fun init() {
        db = Database.connect("jdbc:sqlite:data/data.db", "org.sqlite.JDBC")
    }

    val tables = listOf(
        Users,
        Sessions,
        Teams,
        Amalgamations,
        Tournaments,
        GameCodes,
        TournamentReports,
        TournamentReportTeamPreSelections,
        GameTypes,
        ExtraTimeOptions,
        GameReports,
        Rules,
        DisciplinaryActions,
        Injuries,
        PitchSurfaceOptions,
        PitchLengthOptions,
        PitchWidthOptions,
        PitchMarkingsOptions,
        PitchGoalpostsOptions,
        PitchGoalDimensionOptions,
        Pitches,
        ActivationTokens
    )
    fun createSchema() {
        transaction {
            for (table in tables) {
                SchemaUtils.create(table)
            }

        }
    }

    fun populate_base_data() {
        populate_name_only_table_from_csv(
            ExtraTimeOptions,
            "extra_time_options.csv",
            ExtraTimeOptions.name
        )
        populate_name_only_table_from_csv(
            GameCodes,
            "game_codes.csv",
            GameCodes.name
        )
        populate_name_only_table_from_csv(
            GameTypes,
            "game_types.csv",
            GameTypes.name
        )
        populate_name_only_table_from_csv(
            PitchGoalDimensionOptions,
            "pitch_goal_dimension_options.csv",
            PitchGoalDimensionOptions.name
        )
        populate_name_only_table_from_csv(
            PitchGoalpostsOptions,
            "pitch_goalposts_options.csv",
            PitchGoalpostsOptions.name
        )
        populate_name_only_table_from_csv(
            PitchLengthOptions,
            "pitch_length_options.csv",
            PitchLengthOptions.name
        )
        populate_name_only_table_from_csv(
            PitchMarkingsOptions,
            "pitch_markings_options.csv",
            PitchMarkingsOptions.name
        )
        populate_name_only_table_from_csv(
            PitchSurfaceOptions,
            "pitch_surface_options.csv",
            PitchSurfaceOptions.name
        )
        populate_name_only_table_from_csv(
            PitchWidthOptions,
            "pitch_width_options.csv",
            PitchWidthOptions.name
        )

        populate_rules()

    }

    private fun populate_name_only_table_from_csv(table: LongIdTable, filename: String, nameColumn: Column<String>) {
        val alreadyPopulated = transaction {
            table.selectAll().count() != 0L
        }
        if (!alreadyPopulated) {
            val resource = this.javaClass.classLoader.getResourceAsStream("base_data/$filename")
            resource.use {
                val reader = BufferedReader(
                    InputStreamReader(
                        resource
                    )
                )
                val csvParser = CSVParser(reader, CSVFormat.DEFAULT)
                transaction {
                    for (csvRecord in csvParser) {
                        table.insertAndGetId {
                            it[nameColumn] = csvRecord[0]
                        }
                    }
                }
            }
        }
    }

    private fun populate_rules() {
        val alreadyPopulated = transaction {
            Rules.selectAll().count() != 0L
        }
        if (!alreadyPopulated) {
            val allGameCodes = transaction { GameCode.all().map { it } }
            val hurlingCode = allGameCodes.find { it.name == "Hurling" }
            hurlingCode?.let {
                populate_rules_of_code("hurling.csv",it)
            }
            val ladiesFootball = allGameCodes.find { it.name == "Ladies Football" }
            ladiesFootball?.let {
                populate_rules_of_code("ladiesfootball.csv",it)
            }
            val mensFootball = allGameCodes.find { it.name == "Mens Football" }
            mensFootball?.let {
                populate_rules_of_code("mensfootball.csv",it)
            }
            val camogie = allGameCodes.find { it.name == "Camogie" }
            camogie?.let {
                populate_rules_of_code("camogie.csv",it)
            }
        }
    }

    private fun populate_rules_of_code(filename: String, selcted_code: GameCode) {
        val resource = this.javaClass.classLoader.getResourceAsStream("base_data/rules/$filename")
        resource.use {
            val reader = BufferedReader(
                InputStreamReader(
                    resource
                )
            )
            var format = CSVFormat.Builder
                .create(CSVFormat.EXCEL)
                .setDelimiter(';')
                .setHeader()
                .build()
            val csvParser = CSVParser(reader,format)
            transaction {
                for(csvRecord in csvParser) {
                    Rules.insert {
                        it[code] = selcted_code.id
                        it[isCaution] = csvRecord["isCaution"] == "1"
                        it[isBlack] = csvRecord["isBlack"] == "1"
                        it[isRed] = csvRecord["isRed"] == "1"
                        it[description] = csvRecord["description"]
                    }
                }
            }
        }
    }

}

fun main() {
    DatabaseHandler.populate_base_data()
}


