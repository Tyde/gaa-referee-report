import {defineStore} from "pinia";
import {computed, ref} from "vue";
import type {GameReport, Pitch, Report, ExtraTimeOption, GameType, GameCode, Rule} from "@/types";
import type {PitchVariables} from "@/utils/api/pitch_api";
import {z} from "zod"
import {getRules} from "@/utils/api/disciplinary_action_api";
import {
    completeReportDEOToReport,
    extractGameReportsFromCompleteReportDEO,
    getGameCodes,
    loadReportDEO
} from "@/utils/api/report_api";
import {deletePitchOnServer, getPitchVariables, pitchDEOtoPitch} from "@/utils/api/pitch_api";
import {
    createGameReport,
    deleteGameReportOnServer,
    getGameReportVariables,
    updateGameReport
} from "@/utils/api/game_report_api";
import {checkGameReportMinimal} from "@/utils/gobal_functions";

export const useReportStore = defineStore('report', () => {
    const report = ref<Report>({} as Report)
    report.value.selectedTeams = []

    const gameReports = ref<Array<GameReport>>([])
    const pitchReports = ref<Array<Pitch>>([])

    const codes = ref<Array<GameCode>>([])
    const rules = ref<Array<Rule>>([])
    const gameTypes = ref<Array<GameType>>([])
    const extraTimeOptions = ref<Array<ExtraTimeOption>>([])
    const pitchVariables = ref<PitchVariables | undefined>()

    const currentError = ref<string | undefined>()

    const selectedGameReportIndex = ref<number>(-1)
    const selectedGameReport = computed(() => {
        console.log("Recalc selected game report as sgri is " + selectedGameReportIndex)
        if (selectedGameReportIndex.value >= 0) {
            return gameReports.value[selectedGameReportIndex.value]
        } else {
            return undefined
        }
    })
    const selectedGameReportPassesMinimalRequirements = computed(() => {
        if(selectedGameReport.value != undefined) {
            return checkGameReportMinimal(selectedGameReport.value)
        } else {
            return false
        }
    })

    const selectedPitchReportIndex = ref<number>(-1)
    const selectedPitchReport = computed(() => {
        if (selectedPitchReportIndex.value >= 0) {
            return pitchReports.value[selectedPitchReportIndex.value]
        } else {
            return undefined
        }
    })

    async function loadAuxiliaryInformationFromSerer() {
        const promiseCodes = getGameCodes()
            .then(serverCodes => codes.value = serverCodes)
            .catch(error => currentError.value = error)

        const promiseRules = getRules()
            .then(serverRules => rules.value = serverRules)
            .catch(reason => currentError.value = reason)

        const promisePitchVariables = getPitchVariables()
            .then(serverPitchVariables => pitchVariables.value = serverPitchVariables)
            .catch(reason => currentError.value = reason)

        const promiseGameReport = getGameReportVariables()
            .then(gameReportVariables => {
                gameTypes.value = gameReportVariables.gameTypes
                extraTimeOptions.value = gameReportVariables.extraTimeOptions
            })
            .catch(reason => currentError.value = reason)

        return Promise.all([promiseCodes, promiseRules, promisePitchVariables, promiseGameReport])
    }

    async function loadReport(id: number) {
        try {
            const reportDEOPromise = loadReportDEO(id)
            await loadAuxiliaryInformationFromSerer()
            const reportDEO = await reportDEOPromise
            report.value = completeReportDEOToReport(reportDEO, codes.value)
            gameReports.value = extractGameReportsFromCompleteReportDEO(
                reportDEO,
                report.value,
                gameTypes.value,
                extraTimeOptions.value,
                rules.value
            )
            if(pitchVariables.value) {
                pitchReports.value = reportDEO.pitches?.map(pitch => {
                    return pitchDEOtoPitch(pitch, report.value, pitchVariables.value!!)
                }) ?? []
            }
            return true
        } catch (e) {
            currentError.value = e as string
            return false
        }
    }

    function addNewGameType(gameType: GameType) {
        gameTypes.value.push(gameType)
    }

    async function deleteGameReport(gameReport: GameReport) {
        if(gameReport) {
            if(gameReport.id) {
                await deleteGameReportOnServer(gameReport)
            }
            let index = gameReports.value.indexOf(gameReport)
            gameReports.value.splice(index, 1)
            selectedGameReportIndex.value = Math.min(index, gameReports.value.length-1)
            return true
        }
        return false
    }

    const sendingGameReportCreateRequest = ref(false)
    async function sendGameReport(gameReport: GameReport) {
        if (checkGameReportMinimal(gameReport)) {
            if (gameReport.id) {
                await updateGameReport(gameReport)
            } else {
                if (!sendingGameReportCreateRequest.value) {
                    sendingGameReportCreateRequest.value = true
                    gameReport.id = await createGameReport(gameReport)
                    sendingGameReportCreateRequest.value = false
                }
            }
        }
    }

    async function deletePitchReport(pitchReport: Pitch) {
        if (pitchReport.id) {
            await deletePitchOnServer(pitchReport)
        }
        let index = pitchReports.value.indexOf(pitchReport)
        if (index >= 0) {
            pitchReports.value.splice(index, 1)
            if(pitchReports.value.length > 0) {
                selectedPitchReportIndex.value = Math.min(index, pitchReports.value.length - 1)
            } else {
                selectedPitchReportIndex.value = -1
            }
            return true
        }
        return false
    }

    return {
        report,
        gameReports,
        pitchReports,
        codes,
        rules,
        gameTypes,
        extraTimeOptions,
        pitchVariables,
        currentError,
        selectedGameReportIndex,
        selectedGameReport,
        selectedGameReportPassesMinimalRequirements,
        selectedPitchReportIndex,
        selectedPitchReport,
        loadAuxiliaryInformationFromSerer,
        loadReport,
        addNewGameType,
        deleteGameReport,
        sendGameReport,
        deletePitchReport
    }
})