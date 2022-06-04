package eu.gaelicgames.referee.data

import eu.gaelicgames.referee.data.PitchGoalpostsOptions.default
import eu.gaelicgames.referee.data.User.Companion.referrersOn
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
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

object ExtraTimeReasons : LongIdTable() {
    val name = varchar("name", 60)
}

class ExtraTimeReason(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<ExtraTimeReason>(ExtraTimeReasons)
    var name by ExtraTimeReasons.name
}

object GameReports : LongIdTable() {
    val report = reference("report_id", TournamentReports)
    val teamA = reference("team_a", Teams)
    val teamB = reference("team_b", Teams)
    val startTime = datetime("start_time")
    val gameType = reference("game_type", GameTypes)
    val teamAGoals = integer("team_a_goals")
    val teamAPoints = integer("team_a_points")
    val teamBGoals = integer("team_b_goals")
    val teamBPoints = integer("team_b_points")
    val extraTime = reference("extra_time",ExtraTimeReasons).nullable()
}

class GameReport(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<GameReport>(GameReports)
    var report by TournamentReport referencedOn GameReports.report
    var teamA by Team referencedOn GameReports.teamA
    var teamB by Team referencedOn GameReports.teamB
    var startTime by GameReports.startTime
    var gameType by GameType referencedOn GameReports.gameType
    var teamAGoals by GameReports.teamAGoals
    var teamAPoints by GameReports.teamAPoints
    var teamBGoals by GameReports.teamBGoals
    var teamBPoints by GameReports.teamBPoints
    var extraTime by ExtraTimeReason optionalReferencedOn GameReports.extraTime
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
    val isTeamA = bool("is_team_a")
    val firstName = varchar("first_name",80)
    val lastName = varchar("last_name", 80)
    val number = integer("number")
    val rule = reference("rule",Rules)
    val details = text("details")
}

class DisciplinaryAction(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<DisciplinaryAction>(DisciplinaryActions)
    var game by GameReport referencedOn DisciplinaryActions.game
    var isTeamA by DisciplinaryActions.isTeamA
    var firstName by DisciplinaryActions.firstName
    var lastName by DisciplinaryActions.lastName
    var number by DisciplinaryActions.number
    var rule by Rule referencedOn DisciplinaryActions.rule
    var details by DisciplinaryActions.details
}

object Injuries : LongIdTable() {
    val game = reference("game", GameReports)
    val isTeamA = bool("is_team_a")
    val firstName = varchar("first_name",80)
    val lastName = varchar("last_name", 80)
    val details = text("details")
}

class Injury(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<Injury>(Injuries)
    var game by GameReport referencedOn Injuries.game
    var isTeamA by Injuries.isTeamA
    var firstName by Injuries.firstName
    var lastName by Injuries.lastName
    var details by Injuries.details
}

object PitchSurfaceOptions : LongIdTable() {
    val name = varchar("name",80)
}

class PitchSurfaceOption(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<PitchSurfaceOption>(PitchSurfaceOptions)
    var name by PitchSurfaceOptions.name
}

object PitchLengthOptions : LongIdTable() {
    val name = varchar("name",80)
}

class PitchLengthOption(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<PitchLengthOption>(PitchLengthOptions)
    var name by PitchLengthOptions.name
}

object PitchWidthOptions : LongIdTable() {
    val name = varchar("name",80)
}

class PitchWidthOption(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<PitchWidthOption>(PitchWidthOptions)
    var name by PitchWidthOptions.name
}

object PitchMarkingsOptions : LongIdTable() {
    val name = varchar("name",80)
}

class PitchMarkingsOption(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<PitchMarkingsOption>(PitchMarkingsOptions)
    var name by PitchMarkingsOptions.name
}

object PitchGoalpostsOptions : LongIdTable() {
    val name = varchar("name",80)
}

class PitchGoalpostsOption(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<PitchGoalpostsOption>(PitchGoalpostsOptions)
    var name by PitchGoalpostsOptions.name
}

object PitchGoalDimensionOptions : LongIdTable() {
    val name = varchar("name",80)
}

class PitchGoalDimensionOption(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<PitchGoalDimensionOption>(PitchGoalDimensionOptions)
    var name by PitchGoalDimensionOptions.name
}

object Pitches : LongIdTable() {
    val report = reference("report",TournamentReports)
    val name = varchar("name",80)
    val surface = reference("surface", PitchSurfaceOptions)
    val length = reference("length", PitchLengthOptions)
    val width = reference("width",PitchWidthOptions)
    val smallSquareMarkings = reference("small_square_markings",PitchMarkingsOptions)
    val penaltySquareMarkings = reference("penalty_square_markings",PitchMarkingsOptions)
    val thirteenMeterMarkings = reference("thirteen_meter_markings",PitchMarkingsOptions)
    val twentyMeterMarkings = reference("twenty_meter_markings",PitchMarkingsOptions)
    val longMeterMarkings = reference("long_meter_markings",PitchMarkingsOptions)
    val goalPosts = reference("goal_posts",PitchGoalpostsOptions)
    val goalDimensions = reference("goal_dimensions", PitchGoalDimensionOptions)
    val additionalInformation = text("additional_information")
}

class Pitch(id:EntityID<Long>):LongEntity(id) {
    companion object : LongEntityClass<Pitch>(Pitches)
    var report by TournamentReport referencedOn Pitches.report
    var name by Pitches.name
    var surface by PitchSurfaceOption referencedOn Pitches.surface
    var length by PitchLengthOption referencedOn Pitches.length
    var width by PitchWidthOption referencedOn Pitches.width
    var smallSquareMarkings by PitchMarkingsOption referencedOn Pitches.smallSquareMarkings
    var penaltySquareMarkings by PitchMarkingsOption referencedOn Pitches.penaltySquareMarkings
    var thirteenMeterMarkings by  PitchMarkingsOption referencedOn Pitches.thirteenMeterMarkings
    var twentyMeterMarkings by PitchMarkingsOption referencedOn  Pitches.twentyMeterMarkings
    var longMeterMarkings by PitchMarkingsOption referencedOn Pitches.longMeterMarkings
    var goalPosts by PitchGoalpostsOption referencedOn  Pitches.goalPosts
    var goalDimensions by PitchGoalDimensionOption referencedOn Pitches.goalDimensions
    var additionalInformation by Pitches.additionalInformation
}