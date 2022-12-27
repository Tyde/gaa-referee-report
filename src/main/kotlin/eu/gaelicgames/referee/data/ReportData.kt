package eu.gaelicgames.referee.data

import eu.gaelicgames.referee.data.Team.Companion.optionalBackReferencedOn
import eu.gaelicgames.referee.data.Team.Companion.referrersOn
import eu.gaelicgames.referee.data.api.PitchPropertyDEO
import eu.gaelicgames.referee.resources.Api
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate



object Teams : LongIdTable() {
    val name = varchar("name", 100)
    val isAmalgamation = bool("is_amalgamation")
}

class Team(id:EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Team>(Teams)
    var name by Teams.name
    var isAmalgamation by Teams.isAmalgamation
}

object Amalgamations : LongIdTable() {
    val amalgamation = reference("amalgamation",Teams)
    val addedTeam = reference("added_team",Teams)
}

class Amalgamation(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Amalgamation>(Amalgamations)
    var amalgamation by Team referencedOn Amalgamations.amalgamation
    var addedTeam by Team referencedOn Amalgamations.addedTeam
}

object Tournaments : LongIdTable() {
    val name: Column<String> = varchar("name",80)
    val location: Column<String> = varchar("location",90)
    val date: Column<LocalDate> = date("date")
}

class Tournament(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Tournament>(Tournaments)
    var name by Tournaments.name
    var location by Tournaments.location
    var date by Tournaments.date
}

object GameCodes : LongIdTable() {
    val name = varchar("name",60)
}

class GameCode(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<GameCode>(GameCodes)
    var name by GameCodes.name
}

object TournamentReports : LongIdTable() {
    val tournament = reference("tournament", Tournaments)
    val referee = reference("referee", Users)
    val code = reference("code",GameCodes)
    val additionalInformation = text("additional_information").default("")
    val isSubmitted = bool("is_submitted").default(false)
    val submitDate = datetime("submit_date").nullable()
}

class TournamentReport(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<TournamentReport>(TournamentReports)
    var tournament by Tournament referencedOn TournamentReports.tournament
    var referee by User referencedOn TournamentReports.referee
    var code by GameCode referencedOn TournamentReports.code
    var additionalInformation by TournamentReports.additionalInformation
    var isSubmitted by TournamentReports.isSubmitted
    var submitDate by TournamentReports.submitDate

    val selectedTeams by Team via TournamentReportTeamPreSelections
    val gameReports by GameReport referrersOn GameReports.report
    val pitches by Pitch referrersOn Pitches.report

    fun deleteComplete() {
        val report = this
        transaction {
            TournamentReportTeamPreSelections.deleteWhere {
                TournamentReportTeamPreSelections.report eq report.id
            }
            Pitches.deleteWhere {
                Pitches.report eq report.id
            }
            GameReport.find { GameReports.report eq report.id }.forEach {
                it.deleteComplete()
            }
            report.delete()
        }
    }
}

object TournamentReportTeamPreSelections : LongIdTable() {
    val report = reference("report", TournamentReports)
    val team = reference("team", Teams)



}

class TournamentReportTeamPreSelection(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<TournamentReportTeamPreSelection>(TournamentReportTeamPreSelections)
    var report by TournamentReport referencedOn TournamentReportTeamPreSelections.report
    var team by Team referencedOn TournamentReportTeamPreSelections.team
}

object GameTypes : LongIdTable() {
    val name = varchar("name",60)
}

class GameType(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<GameType>(GameTypes)
    var name by GameTypes.name
}

object ExtraTimeOptions : LongIdTable() {
    val name = varchar("name", 60)
}

class ExtraTimeOption(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<ExtraTimeOption>(ExtraTimeOptions)
    var name by ExtraTimeOptions.name
}



object GameReports : LongIdTable() {
    val report = reference("report_id", TournamentReports)
    val teamA = reference("team_a", Teams)
    val teamB = reference("team_b", Teams)
    val startTime = datetime("start_time").nullable()
    val gameType = reference("game_type", GameTypes).nullable()
    val teamAGoals = integer("team_a_goals")
    val teamAPoints = integer("team_a_points")
    val teamBGoals = integer("team_b_goals")
    val teamBPoints = integer("team_b_points")
    val extraTime = reference("extra_time",ExtraTimeOptions).nullable()
    val umpirePresentOnTime = bool("umpire_present_on_time").default(true)
    val umpireNotes = text("umpire_notes").nullable()
}

class GameReport(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<GameReport>(GameReports)
    var report by TournamentReport referencedOn GameReports.report
    var teamA by Team referencedOn GameReports.teamA
    var teamB by Team referencedOn GameReports.teamB
    var startTime by GameReports.startTime
    var gameType by GameType optionalReferencedOn GameReports.gameType
    var teamAGoals by GameReports.teamAGoals
    var teamAPoints by GameReports.teamAPoints
    var teamBGoals by GameReports.teamBGoals
    var teamBPoints by GameReports.teamBPoints
    var extraTime by ExtraTimeOption optionalReferencedOn GameReports.extraTime
    var umpirePresentOnTime by GameReports.umpirePresentOnTime
    var umpireNotes by GameReports.umpireNotes
    val injuries by Injury referrersOn Injuries.game
    val disciplinaryActions by DisciplinaryAction referrersOn DisciplinaryActions.game


    fun teamADisciplinaryActions():SizedIterable<DisciplinaryAction> {
        return transaction {
            DisciplinaryAction.find {
                (DisciplinaryActions.game eq this@GameReport.id) and
                        (DisciplinaryActions.team eq teamA.id)
            }
        }
    }
    fun teamBDisciplinaryActions():SizedIterable<DisciplinaryAction> {
        return transaction {
            DisciplinaryAction.find {
                (DisciplinaryActions.game eq this@GameReport.id) and
                        (DisciplinaryActions.team eq teamB.id)
            }
        }
    }


    fun teamAInjuries():SizedIterable<Injury> {
        return transaction {
            Injury.find {
                (Injuries.game eq this@GameReport.id) and
                        (Injuries.team eq teamA.id)
            }
        }
    }
    fun teamBInjuries():SizedIterable<Injury> {
        return transaction {
            Injury.find {
                (Injuries.game eq this@GameReport.id) and
                        (Injuries.team eq teamB.id)
            }
        }
    }

    fun deleteComplete() {
        val game = this
        transaction {
            Injuries.deleteWhere {
                Injuries.game eq game.id
            }
            DisciplinaryActions.deleteWhere {
                DisciplinaryActions.game eq game.id
            }
            game.delete()
        }
    }

}

object Rules : LongIdTable() {
    val code = reference("code", GameCodes)
    val isCaution = bool("is_yellow")
    val isBlack = bool("is_black")
    val isRed = bool("is_red")
    val description = text("description")
}

class Rule(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<Rule>(Rules)
    var code by GameCode referencedOn Rules.code
    var isCaution by Rules.isCaution
    var isBlack by Rules.isBlack
    var isRed by Rules.isRed
    var description by Rules.description
}

object DisciplinaryActions : LongIdTable() {
    val game = reference("game",GameReports)
    val team = reference("team",Teams)
    val firstName = varchar("first_name",80)
    val lastName = varchar("last_name", 80)
    val number = integer("number")
    val rule = reference("rule",Rules)
    val details = text("details")
}

class DisciplinaryAction(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<DisciplinaryAction>(DisciplinaryActions)
    var game by GameReport referencedOn DisciplinaryActions.game
    var team by Team referencedOn DisciplinaryActions.team
    var firstName by DisciplinaryActions.firstName
    var lastName by DisciplinaryActions.lastName
    var number by DisciplinaryActions.number
    var rule by Rule referencedOn DisciplinaryActions.rule
    var details by DisciplinaryActions.details
}

object Injuries : LongIdTable() {
    val game = reference("game", GameReports)
    val team = reference("team", Teams)
    val firstName = varchar("first_name",80)
    val lastName = varchar("last_name", 80)
    val details = text("details")
}

class Injury(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<Injury>(Injuries)
    var game by GameReport referencedOn Injuries.game
    var team by Team referencedOn Injuries.team
    var firstName by Injuries.firstName
    var lastName by Injuries.lastName
    var details by Injuries.details
}

interface PitchPropertyEntity {
    var name:String
    fun toPitchPropertyDEO():PitchPropertyDEO
}

object PitchSurfaceOptions : LongIdTable() {
    val name = varchar("name",80)


}

class PitchSurfaceOption(id:EntityID<Long>):LongEntity(id), PitchPropertyEntity {
    companion object : LongEntityClass<PitchSurfaceOption>(PitchSurfaceOptions)
    override var name by PitchSurfaceOptions.name

    override fun toPitchPropertyDEO():PitchPropertyDEO {
        return PitchPropertyDEO(id.value, name)
    }
}

object PitchLengthOptions : LongIdTable() {
    val name = varchar("name",80)
}

class PitchLengthOption(id:EntityID<Long>):LongEntity(id), PitchPropertyEntity {
    companion object : LongEntityClass<PitchLengthOption>(PitchLengthOptions)
    override var name by PitchLengthOptions.name

    override fun toPitchPropertyDEO():PitchPropertyDEO {
        return PitchPropertyDEO(id.value, name)
    }
}

object PitchWidthOptions : LongIdTable() {
    val name = varchar("name",80)
}

class PitchWidthOption(id:EntityID<Long>):LongEntity(id), PitchPropertyEntity {
    companion object : LongEntityClass<PitchWidthOption>(PitchWidthOptions)
    override var name by PitchWidthOptions.name

    override fun toPitchPropertyDEO():PitchPropertyDEO {
        return PitchPropertyDEO(id.value, name)
    }
}

object PitchMarkingsOptions : LongIdTable() {
    val name = varchar("name",80)
}

class PitchMarkingsOption(id:EntityID<Long>):LongEntity(id), PitchPropertyEntity {
    companion object : LongEntityClass<PitchMarkingsOption>(PitchMarkingsOptions)
    override var name by PitchMarkingsOptions.name

    override fun toPitchPropertyDEO():PitchPropertyDEO {
        return PitchPropertyDEO(id.value, name)
    }
}

object PitchGoalpostsOptions : LongIdTable() {
    val name = varchar("name",80)
}

class PitchGoalpostsOption(id:EntityID<Long>):LongEntity(id), PitchPropertyEntity {
    companion object : LongEntityClass<PitchGoalpostsOption>(PitchGoalpostsOptions)
    override var name by PitchGoalpostsOptions.name

    override fun toPitchPropertyDEO():PitchPropertyDEO {
        return PitchPropertyDEO(id.value, name)
    }
}

object PitchGoalDimensionOptions : LongIdTable() {
    val name = varchar("name",80)
}

class PitchGoalDimensionOption(id:EntityID<Long>):LongEntity(id), PitchPropertyEntity {
    companion object : LongEntityClass<PitchGoalDimensionOption>(PitchGoalDimensionOptions)
    override var name by PitchGoalDimensionOptions.name

    override fun toPitchPropertyDEO():PitchPropertyDEO {
        return PitchPropertyDEO(id.value, name)
    }
}

object Pitches : LongIdTable() {
    val report = reference("report",TournamentReports)
    val name = varchar("name",80)
    val surface = optReference("surface", PitchSurfaceOptions)
    val length = optReference("length", PitchLengthOptions)
    val width = optReference("width",PitchWidthOptions)
    val smallSquareMarkings = optReference("small_square_markings",PitchMarkingsOptions)
    val penaltySquareMarkings = optReference("penalty_square_markings",PitchMarkingsOptions)
    val thirteenMeterMarkings = optReference("thirteen_meter_markings",PitchMarkingsOptions)
    val twentyMeterMarkings = optReference("twenty_meter_markings",PitchMarkingsOptions)
    val longMeterMarkings = optReference("long_meter_markings",PitchMarkingsOptions)
    val goalPosts = optReference("goal_posts",PitchGoalpostsOptions)
    val goalDimensions = optReference("goal_dimensions", PitchGoalDimensionOptions)
    val additionalInformation = text("additional_information")
}

class Pitch(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<Pitch>(Pitches)
    var report by TournamentReport referencedOn Pitches.report
    var name by Pitches.name
    var surface by PitchSurfaceOption optionalReferencedOn  Pitches.surface
    var length by PitchLengthOption optionalReferencedOn Pitches.length
    var width by PitchWidthOption optionalReferencedOn Pitches.width
    var smallSquareMarkings by PitchMarkingsOption optionalReferencedOn Pitches.smallSquareMarkings
    var penaltySquareMarkings by PitchMarkingsOption optionalReferencedOn Pitches.penaltySquareMarkings
    var thirteenMeterMarkings by  PitchMarkingsOption optionalReferencedOn Pitches.thirteenMeterMarkings
    var twentyMeterMarkings by PitchMarkingsOption optionalReferencedOn  Pitches.twentyMeterMarkings
    var longMeterMarkings by PitchMarkingsOption optionalReferencedOn Pitches.longMeterMarkings
    var goalPosts by PitchGoalpostsOption optionalReferencedOn  Pitches.goalPosts
    var goalDimensions by PitchGoalDimensionOption optionalReferencedOn Pitches.goalDimensions
    var additionalInformation by Pitches.additionalInformation
}