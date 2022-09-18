package eu.gaelicgames.referee.resources

import eu.gaelicgames.referee.data.GameReports
import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/api")
class Api() {
    @Serializable
    @Resource("teams_available")
    class TeamsAvailable(val parent: Api = Api())

    @Serializable
    @Resource("new_team")
    class NewTeam(val parent: Api)

    @Serializable
    @Resource("new_amalgamation")
    class NewAmalgamation(val parent: Api)

    @Serializable
    @Resource("tournament")
    class Tournaments(val parent: Api) {

        @Serializable
        @Resource("find_by_date/{date}")
        class FindByDate(val parent: Tournaments, val date: String)

        @Serializable
        @Resource("new")
        class New(val parent: Tournaments)
    }

    @Serializable
    @Resource("codes")
    class Codes(val parent: Api)

    @Serializable
    @Resource("rules")
    class Rules(val parent: Api)

    @Serializable
    @Resource("game_report_variables")
    class GameReportVariables(val parent: Api)

    @Serializable
    @Resource("pitch_variables")
    class PitchVariables(val parent: Api)

    @Serializable
    @Resource("rules/{code}")
    class RulesForCode(val parent: Api, val code: Int?)

    @Serializable
    @Resource("report")
    class Reports(val parent: Api) {

        @Serializable
        @Resource("new")
        class New(val parent: Reports)

        @Serializable
        @Resource("update")
        class Update(val parent: Reports)

        @Serializable
        @Resource("get/{id}")
        class Get(val parent: Reports, val id: Long)

        @Serializable
        @Resource("updateAdditionalInformation")
        class UpdateAdditionalInformation(val parent: Reports)

        @Serializable
        @Resource("submit")
        class Submit(val parent: Reports)
    }

    @Serializable
    @Resource("gamereport")
    class GameReports(val parent: Api) {

        @Serializable
        @Resource("new")
        class New(val parent: GameReports)

        @Serializable
        @Resource("update")
        class Update(val parent: GameReports)

        @Serializable
        @Resource("delete")
        class Delete(val parent: GameReports)

        @Serializable
        @Resource("disciplinaryAction")
        class DisciplinaryAction(val parent: GameReports) {
            @Serializable
            @Resource("new")
            class New(val parent: DisciplinaryAction)

            @Serializable
            @Resource("update")
            class Update(val parent: DisciplinaryAction)
        }

        @Serializable
        @Resource("injury")
        class Injury(val parent: GameReports) {
            @Serializable
            @Resource("new")
            class New(val parent: Injury)

            @Serializable
            @Resource("update")
            class Update(val parent: Injury)
        }


    }

    @Serializable
    @Resource("gametype")
    class GameType(val parent: Api) {
        @Serializable
        @Resource("new")
        class New(val parent: GameType)

        @Serializable
        @Resource("update")
        class Update(val parent: GameType)
    }
    @Serializable
    @Resource("pitch")
    class Pitch(val parent: Api) {
        @Serializable
        @Resource("new")
        class New(val parent: Pitch)

        @Serializable
        @Resource("update")
        class Update(val parent: Pitch)

        @Serializable
        @Resource("delete")
        class Delete(val parent: Pitch)
    }


}