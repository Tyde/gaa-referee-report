package eu.gaelicgames.referee.util

import eu.gaelicgames.referee.data.User
import eu.gaelicgames.referee.data.UserRole
import eu.gaelicgames.referee.data.Users
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jetbrains.exposed.sql.transactions.transaction

object MockDataGenerator {
    fun addMockUsers() {
        val resource = this.javaClass.classLoader.getResourceAsStream("mock_data/users.csv")
        resource.use {
            it.bufferedReader().let { buf ->
                val csvParser = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(buf)
                println(csvParser.headerNames);
                transaction {
                    var firstCheckDone = false;
                    for (csvRecord in csvParser) {
                        if(!firstCheckDone) {
                            if(User.find { Users.mail eq csvRecord.get(4) }.count() > 0) {
                                println("Users already populated")
                                break
                            } else {
                                firstCheckDone = true
                            }
                        }
                        User.newWithPassword(
                            csvRecord.get(1),
                            csvRecord.get(2),
                            csvRecord.get(4),
                            csvRecord.get(3),
                            UserRole.REFEREE
                        )
                        println("Adding User: ${csvRecord.get(1)} ${csvRecord.get(2)} ${csvRecord.get(4)} ${csvRecord.get(3)}")
                    }
                }

            }
        }

    }
}