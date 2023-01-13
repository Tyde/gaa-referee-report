import PitchOptionsEditor from "@/components/admin/PitchOptionsEditor.vue";
import GameReportOptionsEditor from "@/components/admin/GameReportOptionsEditor.vue";


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
    }
]