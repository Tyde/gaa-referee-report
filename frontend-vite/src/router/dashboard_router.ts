import UserReportList from "@/components/dashboard/UserReportList.vue";
import UserHome from "@/components/dashboard/UserHome.vue";
import UserProfile from "@/components/dashboard/UserProfile.vue";

export const routes = [
    {
        path: "/",
        component: UserHome
    },
    {
        path: "/profile",
        component: UserProfile
    }
]