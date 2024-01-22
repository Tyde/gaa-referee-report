package eu.gaelicgames.referee.resources

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/api")
class Api() {

    @Serializable
    @Resource("login")
    class Login(val parent: Api)

    @Serializable
    @Resource("session")
    class Session(val parent: Api)

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
    @Resource("team")
    class Team(val parent: Api) {

        @Serializable
        @Resource("update")
        class Update(val parent: Team)


        @Serializable
        @Resource("merge")
        class Merge(val parent: Team)
    }

    @Serializable
    @Resource("tournament")
    class Tournaments(val parent: Api) {

        @Serializable
        @Resource("find_by_date/{date}")
        class FindByDate(val parent: Tournaments, val date: String)

        @Serializable
        @Resource("new")
        class New(val parent: Tournaments)

        @Serializable
        @Resource("all")
        class All(val parent: Tournaments)

        @Serializable
        @Resource("update")
        class Update(val parent: Tournaments)

        @Serializable
        @Resource("complete_report_public/{id}")
        class CompleteReportPublic(val parent: Tournaments, val id: Long)

        @Serializable
        @Resource("complete_report/{id}")
        class CompleteReport(val parent: Tournaments, val id: Long)

    }

    @Serializable
    @Resource("region")
    class Regions(val parent: Api) {

        @Serializable
        @Resource("all")
        class All(val parent: Regions)
    }

    @Serializable
    @Resource("codes")
    class Codes(val parent: Api)

    @Serializable
    @Resource("rules")
    class Rules(val parent: Api)

    @Serializable
    @Resource("rule")
    class Rule(val parent:Api) {
        @Serializable
        @Resource("new")
        class New(val parent: Rule)

        @Serializable
        @Resource("update")
        class Update(val parent: Rule)

        @Serializable
        @Resource("disable")
        class Disable(val parent: Rule)

        @Serializable
        @Resource("enable")
        class Enable(val parent: Rule)

        @Serializable
        @Resource("delete")
        class Delete(val parent: Rule)

        @Serializable
        @Resource("check_deletable")
        class CheckDeletable(val parent: Rule)
    }


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
        @Resource("all")
        class All(val parent: Reports)

        @Serializable
        @Resource("my")
        class My(val parent: Reports)

        @Serializable
        @Resource("updateAdditionalInformation")
        class UpdateAdditionalInformation(val parent: Reports)

        @Serializable
        @Resource("submit")
        class Submit(val parent: Reports)

        @Serializable
        @Resource("delete")
        class Delete(val parent: Reports)

        @Serializable
        @Resource("share")
        class Share(val parent: Reports)

        @Serializable
        @Resource("get_shared/{uuid}")
        class GetShared(val parent: Reports, val uuid: String)
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

            @Serializable
            @Resource("delete")
            class Delete(val parent: DisciplinaryAction)
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

            @Serializable
            @Resource("delete")
            class Delete(val parent: Injury)
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


    @Serializable
    @Resource("pitch_property")
    class PitchProperty(val parent: Api) {
        @Serializable
        @Resource("new")
        class New(val parent: PitchProperty)

        @Serializable
        @Resource("update")
        class Update(val parent: PitchProperty)

        @Serializable
        @Resource("delete")
        class Delete(val parent: PitchProperty)

        @Serializable
        @Resource("enable")
        class Enable(val parent: PitchProperty)
    }

    @Serializable
    @Resource("user")
    class User(val parent: Api) {
        @Serializable
        @Resource("all")
        class All(val parent: User)

        @Serializable
        @Resource("update")
        class Update(val parent: User)

        @Serializable
        @Resource("update_me")
        class UpdateMe(val parent: User)

        @Serializable
        @Resource("update_password")
        class UpdatePassword(val parent: User)

        @Serializable
        @Resource("new")
        class New(val parent: User)

        @Serializable
        @Resource("validate_activation_token")
        class ValidateActivationToken(val parent: User)

        @Serializable
        @Resource("activate")
        class Activate(val parent: User)



        @Serializable
        @Resource("set_role")
        class SetRole(val parent: User)


    }

}
