package eu.gaelicgames.referee.util

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eu.gaelicgames.referee.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.BufferedReader
import java.io.InputStreamReader

val USE_POSTGRES = true

object DatabaseHandler {
    lateinit var pool: HikariDataSource
    var db: Database? = null
    fun init(testing: Boolean = false, usePostgres: Boolean = USE_POSTGRES) {
        if (testing) {
            db = if (usePostgres) {
                println("Trying to connect to postgres")
                println("Host: ${GGERefereeConfig.postgresHost}:${GGERefereeConfig.postgresPort}")
                Database.connect(
                    "jdbc:postgresql://${GGERefereeConfig.postgresHost}:${GGERefereeConfig.postgresPort}/testing",
                    driver = "org.postgresql.Driver",
                    user = "root",
                    password = "testing"
                )
            } else {
                Database.connect("jdbc:sqlite:data/test.db", "org.sqlite.JDBC")
            }
        } else {
            db = if (usePostgres) {
                val config = DatabaseConfig {
                    useNestedTransactions = false
                }

                /*

                val poolConfig = HikariConfig().apply {
                    jdbcUrl = "jdbc:postgresql://${GGERefereeConfig.postgresHost}:${GGERefereeConfig.postgresPort}/${GGERefereeConfig.postgresDatabase}"
                    maximumPoolSize = 2
                    isReadOnly = false
                    transactionIsolation = "TRANSACTION_SERIALIZABLE"
                    driverClassName = "org.postgresql.Driver"
                    username = GGERefereeConfig.postgresUser
                    password = GGERefereeConfig.postgresPassword
                    isAutoCommit = false
                }


                pool = HikariDataSource(poolConfig)
                Database.connect(
                    datasource = pool,
                    databaseConfig = config
                )*/


                Database.connect(
                    "jdbc:postgresql://${GGERefereeConfig.postgresHost}:${GGERefereeConfig.postgresPort}/${GGERefereeConfig.postgresDatabase}",
                    driver = "org.postgresql.Driver",
                    user = GGERefereeConfig.postgresUser,
                    password = GGERefereeConfig.postgresPassword,
                    databaseConfig = config
                )
            } else {
                Database.connect("jdbc:sqlite:data/data.db", "org.sqlite.JDBC")
            }

        }
    }

    val tables = listOf(
        Users,
        Sessions,
        Teams,
        Amalgamations,
        Regions,
        Tournaments,
        GameCodes,
        TournamentReports,
        TournamentReportTeamPreSelections,
        TournamentTeamPreSelections,
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
        ActivationTokens,
        TournamentReportShareLinks
    )

    suspend fun createSchema() {
        lockedTransaction {
            for (table in tables) {
                SchemaUtils.create(table)
            }
            //Migration 1 - All PitchProperties now have a "disabled" field
            SchemaUtils.createMissingTablesAndColumns(
                PitchSurfaceOptions,
                PitchLengthOptions,
                PitchWidthOptions,
                PitchMarkingsOptions,
                PitchGoalpostsOptions,
                PitchGoalDimensionOptions
            )

            //Migration 2 - All Tournaments now have a "region" field
            SchemaUtils.createMissingTablesAndColumns(Tournaments)

            //Migration 3 - Add "general_notes" field to GameReports
            SchemaUtils.createMissingTablesAndColumns(GameReports)

            //Migration 4 - Add "redCardIssued" to DisciplinaryActions
            SchemaUtils.createMissingTablesAndColumns(DisciplinaryActions)

            //Migration 5 - Add league field to Tournaments
            SchemaUtils.createMissingTablesAndColumns(Tournaments)
        }
    }

    suspend fun populate_base_data() {
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
        populate_name_only_table_from_csv(
            Regions,
            "regions.csv",
            Regions.name
        )

        populate_rules()

    }

    private suspend fun populate_name_only_table_from_csv(table: LongIdTable, filename: String, nameColumn: Column<String>) {
        val alreadyPopulated = lockedTransaction {
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
                lockedTransaction {
                    for (csvRecord in csvParser) {
                        table.insertAndGetId {
                            it[nameColumn] = csvRecord[0]
                        }
                    }
                }
            }
        }
    }

    private suspend fun populate_rules() {
        val alreadyPopulated = lockedTransaction {
            Rules.selectAll().count() != 0L
        }
        if (!alreadyPopulated) {
            val allGameCodes = lockedTransaction { GameCode.all().map { it } }
            val hurlingCode = allGameCodes.find { it.name == "Hurling" }
            hurlingCode?.let {
                populate_rules_of_code("hurling.csv", it)
            }
            val ladiesFootball = allGameCodes.find { it.name == "Ladies Football" }
            ladiesFootball?.let {
                populate_rules_of_code("ladiesfootball.csv", it)
            }
            val mensFootball = allGameCodes.find { it.name == "Mens Football" }
            mensFootball?.let {
                populate_rules_of_code("mensfootball.csv", it)
            }
            val camogie = allGameCodes.find { it.name == "Camogie" }
            camogie?.let {
                populate_rules_of_code("camogie.csv", it)
            }
        }
    }

    private suspend fun populate_rules_of_code(filename: String, selcted_code: GameCode) {
        val resource = this.javaClass.classLoader.getResourceAsStream("base_data/rules/$filename")
        resource.use {
            val reader = BufferedReader(
                InputStreamReader(
                    resource
                )
            )
            val format = CSVFormat.Builder
                .create(CSVFormat.EXCEL)
                .setDelimiter(';')
                .setHeader()
                .build()
            val csvParser = CSVParser(reader, format)
            lockedTransaction {
                for (csvRecord in csvParser) {
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

    data class DependencyNode(
        val table: LongIdTable,
        val dependsOn: MutableList<DependencyNode>,
        val isReferencedBy: MutableList<DependencyNode> = mutableListOf()
    )

    fun constructDependencyTree(): List<DependencyNode> {
        val nodes = tables.map { it -> DependencyNode(it, mutableListOf(), mutableListOf()) }
        tables.forEach { table ->
            val dependsOn = table.foreignKeys.map { it.targetTable }.distinct()
            val dependsOnNodes = dependsOn.map { targetTable ->
                nodes.find { it.table == targetTable }
                    ?: throw Exception("Table $table depends on $targetTable, but $targetTable is not in the list of tables")
            }
            val myNode =
                nodes.find { it.table == table } ?: throw Exception("Table $table is not in the list of tables")
            myNode.dependsOn.addAll(dependsOnNodes)
            dependsOnNodes.forEach { it.isReferencedBy.add(myNode) }
        }
        return nodes
    }

    fun migrateSQLiteToPostgres() {
        val sqliteDB = Database.connect("jdbc:sqlite:data/data.db", "org.sqlite.JDBC")
        val postgresDB = Database.connect(
            "jdbc:postgresql://${GGERefereeConfig.postgresHost}:${GGERefereeConfig.postgresPort}/${GGERefereeConfig.postgresDatabase}",
            driver = "org.postgresql.Driver",
            user = GGERefereeConfig.postgresUser,
            password = GGERefereeConfig.postgresPassword
        )

        transaction(postgresDB) {
            for (table in tables) {
                SchemaUtils.create(table)
            }
        }





        transaction(sqliteDB) {
            val treeNodes = constructDependencyTree()
            val tablesMigrated = mutableListOf<Table>()
            val tableCount = tables.size
            while (tablesMigrated.size < tableCount) {
                val tablesToMigrate = treeNodes.filter { node ->
                    node.dependsOn.all { it.table in tablesMigrated }
                }.filter { it.table !in tablesMigrated }
                println("Next set of tables to migrate:")
                tablesToMigrate.forEach { println(it.table.tableName) }
                for (tableNode in tablesToMigrate) {
                    val table = tableNode.table
                    println("Migrating ${table.tableName}")
                    val insertStatement = "INSERT INTO ${tableNode.table.tableName} ("
                    val columns = tableNode.table.columns.joinToString(",") { "\"" + it.name + "\"" }
                    val values = tableNode.table.columns.joinToString(",") { "?" }
                    val insertStatementEnd = ") VALUES ($values);"
                    val statementString = insertStatement + columns + insertStatementEnd
                    println(statementString)
                    table.selectAll().map {
                        val params = table.columns.map { column ->
                            Pair(column.columnType, it[column])
                        }
                        println("Inserting $params")
                        transaction(postgresDB) {
                            try {
                                val statement = this.connection.prepareStatement(
                                    statementString,
                                    false
                                )
                                statement.fillParameters(params)
                                statement.executeUpdate()
                            } catch (e: Exception) {
                                println("Error inserting $params")
                                e.printStackTrace()
                            }

                        }
                    }

                    transaction(postgresDB) {
                        val maxval = table.slice(table.id.max())
                            .selectAll()
                            .firstOrNull()
                            ?.get(table.id.max())

                        maxval?.let {
                            val sequenceName = "${table.tableName}_${table.id.name}_seq"
                            val nextVal = "SELECT setval('$sequenceName', ${maxval.value + 1});"
                            this.connection.prepareStatement(nextVal, arrayOf("setval")).executeQuery()

                        }
                    }

                    tablesMigrated.add(tableNode.table)
                }

            }

        }


    }

}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> lockedTransaction(statement: suspend Transaction.() -> T): T {
    return newSuspendedTransaction(Dispatchers.IO,DatabaseHandler.db) {
        val t = statement()
        commit()
        t
    }

}

fun main() {
    /*DatabaseHandler.init(testing = false, usePostgres = false)
    DatabaseHandler.createSchema()
    transaction {
        val treeNodes = DatabaseHandler.constructDependencyTree()

        val tablesMigrated = mutableListOf<Table>()
        val tableCount = DatabaseHandler.tables.size
        while (tablesMigrated.size < tableCount) {
            val tablesToMigrate = treeNodes.filter { it.dependsOn.all { it.table in tablesMigrated } }
            for (tableNode in tablesToMigrate) {
                println("Migrating ${tableNode.table.tableName}")
                val res = tableNode.table.selectAll().map { row ->
                    val insertStatement = "INSERT INTO ${tableNode.table.tableName} ("
                    val columns = tableNode.table.columns.joinToString(",") { it.name }
                    val values = tableNode.table.columns.joinToString(",") { "?" }
                    val insertStatementEnd = ") VALUES ($values)"
                    insertStatement + columns + insertStatementEnd
                }
                println(res)
                tablesMigrated.add(tableNode.table)
            }
        }
    }*/
    DatabaseHandler.migrateSQLiteToPostgres()


}


