import UserHome from "@/components/dashboard/UserHome.vue";
import UserProfile from "@/components/dashboard/UserProfile.vue";
import ShowReportApp from "@/ShowReportApp.vue";
import TournamentReportList from "@/components/admin/TournamentReportList.vue";
import FullTournamentReport from "@/components/admin/tournaments/FullTournamentReport.vue";
import AdminShowReport from "@/components/admin/AdminShowReport.vue";

export const routes = [
    {
        path: "/",
        component: UserHome
    },
    {
        path: "/profile",
        component: UserProfile
    },
    {
        path: "/report/:id",
        component: ShowReportApp,
        props: true
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
    }
]
