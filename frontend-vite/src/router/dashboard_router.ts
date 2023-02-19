import UserReportList from "@/components/dashboard/UserReportList.vue";
import UserHome from "@/components/dashboard/UserHome.vue";
import UserProfile from "@/components/dashboard/UserProfile.vue";
import ShowReportApp from "@/ShowReportApp.vue";

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
    }
]