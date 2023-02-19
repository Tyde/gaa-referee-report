import {defineStore} from "pinia";
import { ref} from "vue";
import {ErrorMessage, GameCode} from "@/types";
import type {CompactTournamentReportDEO} from "@/utils/api/report_api";
import {deleteReportOnServer, getGameCodes, loadMyReports} from "@/utils/api/report_api";
import {loadAllTournaments} from "@/utils/api/tournament_api";
import {getSessionInfo, updateMeUser} from "@/utils/api/referee_api";
import type {DatabaseTournament} from "@/types/tournament_types";
import type {Referee} from "@/types/referee_types";


export const useDashboardStore = defineStore('dashboard', () => {
    const currentErrors = ref<ErrorMessage[]>([])
    const codes = ref<Array<GameCode>>([])
    const tournaments = ref<Array<DatabaseTournament>>([])

    const isAdmin = ref(false)
    const me = ref<Referee>({} as Referee)



    const isLoading = ref<boolean>(false)
    function newError(message: string) {
        currentErrors.value.push(new ErrorMessage(message))
    }

    const myReports = ref<Array<CompactTournamentReportDEO>>([])

    async function fetchMyReports() {
        isLoading.value = true
        let gcProm = getGameCodes()
            .then(serverCodes => codes.value = serverCodes)
            .catch(reason => newError(reason))
        let myRepProm = loadMyReports()
            .then(reports => myReports.value = reports)
            .catch(reason => newError(reason))
        let allTournProm = loadAllTournaments()
            .then(data => tournaments.value = data)
            .catch(reason => newError(reason))
        await Promise.all([gcProm, myRepProm, allTournProm])
        isLoading.value = false
    }

    async function checkAdmin() {
        getSessionInfo()
            .then(sessionInfo => me.value = sessionInfo)
            .then(sessionInfo => isAdmin.value = sessionInfo.role == "ADMIN")
            .catch(reason => newError(reason))
    }

    async function deleteReport(report: CompactTournamentReportDEO) {
        isLoading.value = true
        await deleteReportOnServer(report.id)
            .catch(reason => newError(reason))
        await loadMyReports()
            .then(reports => myReports.value = reports)
            .catch(reason => newError(reason))
        isLoading.value = false
    }
    function findCodeById(id: number) {
        return codes.value.find(it => it.id === id)
    }

    function findTournamentById(id: number) {
        return tournaments.value.find(it => it.id === id)
    }


    async function updateMe(shadowCopy: Referee) {
        try{
            let newUser = await updateMeUser(shadowCopy)
            me.value = shadowCopy
        } catch (e: any) {
            newError(e)
        }
    }


    return {
        currentErrors,
        codes,
        myReports,
        isLoading,
        isAdmin,
        me,
        newError,
        fetchMyReports,
        findCodeById,
        checkAdmin,
        findTournamentById,
        deleteReport,
        updateMe
    }
})