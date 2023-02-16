import {defineStore} from "pinia";
import {ref} from "vue";
import {DatabaseTournament, ErrorMessage, GameCode} from "@/types";
import type {CompactTournamentReportDEO} from "@/utils/api/report_api";
import {deleteReportOnServer, getGameCodes, loadMyReports} from "@/utils/api/report_api";
import {loadAllTournaments} from "@/utils/api/tournament_api";
import {getSessionInfo} from "@/utils/api/referee_api";


export const useDashboardStore = defineStore('dashboard', () => {
    const currentErrors = ref<ErrorMessage[]>([])
    const codes = ref<Array<GameCode>>([])
    const tournaments = ref<Array<DatabaseTournament>>([])

    const isAdmin = ref(false)

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



    return {
        currentErrors,
        codes,
        myReports,
        isLoading,
        isAdmin,
        newError,
        fetchMyReports,
        findCodeById,
        checkAdmin,
        findTournamentById,
        deleteReport
    }
})