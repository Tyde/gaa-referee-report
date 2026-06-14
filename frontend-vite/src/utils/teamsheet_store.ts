import {defineStore} from "pinia";
import {usePublicStore} from "@/utils/public_store";
import {ref} from "vue";
import type {TeamsheetUploadSuccessDEO} from "@/types/teamsheet_types";

export const useTeamsheetStore = defineStore("teamsheet", () => {

    const publicStore = usePublicStore()

    const uploadSuccessDEO = ref<TeamsheetUploadSuccessDEO>()

    function newError(message: string) {
        publicStore.newError(message)
    }

    return {
        publicStore,
        uploadSuccessDEO,
        newError
    }
})
