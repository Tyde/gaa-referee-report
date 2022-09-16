import PitchOptionsEditor from "@/components/admin/PitchOptionsEditor.vue";


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
        path: "/pitch-report-options",
    }
]