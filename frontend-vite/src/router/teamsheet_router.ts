import UserHome from "@/components/dashboard/UserHome.vue";
import UserProfile from "@/components/dashboard/UserProfile.vue";
import ShowReportApp from "@/ShowReportApp.vue";
import TournamentReportList from "@/components/admin/TournamentReportList.vue";
import FullTournamentReport from "@/components/admin/tournaments/FullTournamentReport.vue";
import AdminShowReport from "@/components/admin/AdminShowReport.vue";
import SetupFutureTournament from "@/components/tournament/SetupFutureTournament.vue";
import AugmentTeamsheetData from "@/components/teamsheet/AugmentTeamsheetData.vue";
import TeamsheetComplete from "@/components/teamsheet/TeamsheetComplete.vue";
import UploadTeamsheetPage from "@/components/teamsheet/UploadTeamsheetPage.vue";

export const routes = [
    {
        path: "/",
        component: UploadTeamsheetPage
    },
    {
        name: "augment-data",
        path: "/augment-data/:fileKey",
        component: AugmentTeamsheetData,
        props: true
    },
    {
        name: "teamsheet-complete",
        path: "/teamsheet-complete/:fileKey",
        component: TeamsheetComplete
    }
]
