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
