package eu.gaelicgames.referee.util

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {

    //DatabaseHandler.init()
    printStatements()
    //runMigrations()
}

fun printStatements() {
    DatabaseHandler.db = Database.connect("jdbc:sqlite:datatesqt.db", "org.sqlite.JDBC")
    transaction {
        DatabaseHandler.tables.flatMap {
            SchemaUtils.createStatements(it)
        }.toSet().forEach{println(it)}
    }
}

fun runMigrations() {
    val flyway = Flyway.configure().dataSource("jdbc:sqlite:datatest.db","","").load()
    flyway.migrate()
}