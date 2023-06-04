import PublicTournamentList from "@/components/public/PublicTournamentList.vue";
import ShowPublicTournamentReport from "@/components/public/ShowPublicTournamentReport.vue";

export const routes = [
    {
        path: "/",
        component: PublicTournamentList
    },
    {
        path: "/tournament/:id",
        props: true,
        component: ShowPublicTournamentReport
    }
]