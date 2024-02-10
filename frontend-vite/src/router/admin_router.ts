import PitchOptionsEditor from "@/components/admin/PitchOptionsEditor.vue";
import GameReportOptionsEditor from "@/components/admin/GameReportOptionsEditor.vue";
import TournamentReportList from "@/components/admin/TournamentReportList.vue";
import AdminShowReport from "@/components/admin/AdminShowReport.vue";
import UserList from "@/components/admin/user/UserList.vue";
import TeamManager from "@/components/admin/teams/TeamManager.vue";
import RulesEditor from "@/components/admin/gameReport/RulesEditor.vue";
import TournamentList from "@/components/admin/tournaments/TournamentList.vue";
import FullTournamentReport from "@/components/admin/tournaments/FullTournamentReport.vue";


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
        path: "/tournament-reports/complete/:id",
        component: FullTournamentReport,
        props: true
    },
    {
        path: "/tournaments",
        component: TournamentList
    },
    {
        path: "/referees/",
        component: UserList,
        props: {refereeMode: true}
    },
    {
        path: "/ccc/",
        component: UserList,
        props: {refereeMode: false}
    },
    {
        path: "/teams",
        component: TeamManager,
    }
]
