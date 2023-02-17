import PitchOptionsEditor from "@/components/admin/PitchOptionsEditor.vue";
import GameReportOptionsEditor from "@/components/admin/GameReportOptionsEditor.vue";
import TournamentReportList from "@/components/admin/TournamentReportList.vue";
import ShowPitchReport from "@/components/showReport/ShowPitchReport.vue";
import ShowReportApp from "@/ShowReportApp.vue";
import AdminShowReport from "@/components/admin/AdminShowReport.vue";
import UserList from "@/components/admin/user/UserList.vue";
import TeamList from "@/components/admin/teams/TeamList.vue";
import TeamManager from "@/components/admin/teams/TeamManager.vue";
import RulesEditor from "@/components/admin/gameReport/RulesEditor.vue";


export const routes = [
    {
        path: "/",
        redirect: "pitch-options"
    },
    {
        path: "/pitch-options",
        component: PitchOptionsEditor,
    },
    {
        path: "/game-report-options",
        component:  GameReportOptionsEditor,
    },
    {
        path: "/rules",
        component: RulesEditor
    },
    {
        path: "/tournament-reports",
        component: TournamentReportList
    },
    {
        path: "/tournament-reports/:id",
        component: AdminShowReport,
        props: true
    },
    {
        path: "/referees/",
        component: UserList,
    },
    {
        path: "/teams",
        component: TeamManager,
    }
]