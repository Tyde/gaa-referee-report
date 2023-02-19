import {defineStore} from "pinia";
import {computed, ref} from "vue";
import type {
    GameReport,
    Pitch,
    Report,
    ExtraTimeOption,
    GameType,
    GameCode,
    Rule,
    DisciplinaryAction,
    Injury
} from "@/types";
import type {PitchVariables} from "@/utils/api/pitch_api";
import {getRules, uploadDisciplinaryAction} from "@/utils/api/disciplinary_action_api";
import {
    completeReportDEOToReport,
    extractGameReportsFromCompleteReportDEO,
    getGameCodes,
    loadReportDEO
} from "@/utils/api/report_api";
import {deletePitchOnServer, getPitchVariables, pitchDEOtoPitch, uploadPitch} from "@/utils/api/pitch_api";
import {
    createGameReport,
    deleteGameReportOnServer,
    getGameReportVariables,
    updateGameReport
} from "@/utils/api/game_report_api";
import {checkGameReportMinimal} from "@/utils/gobal_functions";
import {ErrorMessage} from "@/types";
import {uploadInjury} from "@/utils/api/injuries_api";

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

    const currentErrors = ref<ErrorMessage[]>([])



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
            .catch(error => currentErrors.value.push(new ErrorMessage(error)))

        const promiseRules = getRules()
            .then(serverRules => rules.value = serverRules)
            .catch(reason => currentErrors.value.push(new ErrorMessage(reason)))

        const promisePitchVariables = getPitchVariables()
            .then(serverPitchVariables => pitchVariables.value = serverPitchVariables)
            .catch(reason => currentErrors.value.push(new ErrorMessage(reason)))

        const promiseGameReport = getGameReportVariables()
            .then(gameReportVariables => {
                gameTypes.value = gameReportVariables.gameTypes
                extraTimeOptions.value = gameReportVariables.extraTimeOptions
            })
            .catch(reason => currentErrors.value.push(new ErrorMessage(reason)))

        return Promise.all([promiseCodes, promiseRules, promisePitchVariables, promiseGameReport])
    }

    async function loadReport(id: number) {
        try {
            const reportDEOPromise = loadReportDEO(id)
                .catch((error) => {
                    newError(error)
                    return undefined
                })
            await loadAuxiliaryInformationFromSerer()
            const reportDEO = await reportDEOPromise
            if (reportDEO) {
                report.value = completeReportDEOToReport(reportDEO, codes.value)
                gameReports.value = extractGameReportsFromCompleteReportDEO(
                    reportDEO,
                    report.value,
                    gameTypes.value,
                    extraTimeOptions.value,
                    rules.value
                )
                if (pitchVariables.value) {
                    pitchReports.value = reportDEO.pitches?.map(pitch => {
                        return pitchDEOtoPitch(pitch, report.value, pitchVariables.value!!)
                    }) ?? []
                }
                return true
            }
        } catch (e) {
            currentErrors.value.push(new ErrorMessage(e as string))
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
                    .catch((error) => {
                        newError(error)
                    })
            }
            let index = gameReports.value.indexOf(gameReport)
            gameReports.value.splice(index, 1)
            selectedGameReportIndex.value = Math.min(index, gameReports.value.length-1)
            return true
        }
        return false
    }
    const currentTransfers = ref<Array<Promise<any>>>([])

    /**
     * Creates a new game report and adds it to the list of game reports.
     *
     *  Warning! Only use allowAsync if you already checked before that
     *        all transfers are completed.
     * @param gameReport the game report to be created
     * @param allowAsync if false, the function will wait until all transfers are completed before sending the request
     */
    async function sendGameReport(gameReport: GameReport, allowAsync: boolean = false) {

        if(!allowAsync) await waitForAllTransfersDone()
        if (checkGameReportMinimal(gameReport)) {
            if (gameReport.id) {

                await trackTransfer(updateGameReport(gameReport))
                    .catch((error) => {
                        newError(error)
                    })
            } else {
                gameReport.id = await trackTransfer(createGameReport(gameReport))
            }
        }
    }

    /**
     *  Sends all pitches to the server.
     *
     *  Warning! Only use allowAsync if you already checked before that
     *      all transfers are completed.
     * @param pitchReport the pitch report to be created/updated
     * @param allowAsync if false, the function will wait until all transfers are completed before sending the request
     */
    async function sendPitchReport(pitchReport: Pitch, allowAsync: boolean = false) {
        //Warning! Only use allowAsync if you already checked before that
        // all transfers are completed.
        if(!allowAsync) await waitForAllTransfersDone()
        return trackTransfer(uploadPitch(pitchReport))
    }

    /**
     * Sends DisciplinaryAction to server - Does not check for correctness of the action
     *
     * This function will update the id of the action if it was created on the server.
     *  Warning! Only use allowAsync if you already checked before that
     *      all transfers are completed.
     * @param disciplinaryAction the action to be created/updated
     * @param gameReport the game report to which the action belongs
     * @param allowAsync if false, the function will wait until all transfers are completed before sending the request
     */
    async function sendDisciplinaryAction(disciplinaryAction: DisciplinaryAction, gameReport: GameReport, allowAsync: boolean = false) {
        if(!allowAsync) await waitForAllTransfersDone()
        if (gameReport.id) {
            let hasNoId = disciplinaryAction.id == undefined
            await trackTransfer(uploadDisciplinaryAction(disciplinaryAction, gameReport.id))
                .then((data) => {
                    if (hasNoId && data != -1) {
                        disciplinaryAction.id = data
                    }
                })
        }
    }

    /**
     * Sends Injury to server - Does not check for correctness of the action
     * This function will update the id of the action if it was created on the server.
     *
     * Warning! Only use allowAsync if you already checked before that
     *     all transfers are completed.
     * @param injury the injury to be created/updated
     * @param gameReport the game report to which the injury belongs
     * @param allowAsync if false, the function will wait until all transfers are completed before sending the request
     */
    async function sendInjury(injury: Injury, gameReport: GameReport, allowAsync: boolean = false) {
        if(!allowAsync) await waitForAllTransfersDone()
        if (gameReport.id) {
            let hasNoId = injury.id == undefined
            await trackTransfer(uploadInjury(injury, gameReport.id))
                .then((data) => {
                    if (hasNoId && data != -1) {
                        injury.id = data
                    }
                })
        }
    }

    /**
     * Helper function to track the current promises waiting for a response from the server.
     *
     * This is used for waitForAllTransfersDone
     * @param promise the promise to be tracked
     */
    async function trackTransfer<T>(promise: Promise<T>):Promise<T> {
        currentTransfers.value.push(promise)
        promise.then(() => {
            currentTransfers.value.splice(currentTransfers.value.indexOf(promise), 1)
        }).catch((error) => {
            currentTransfers.value.splice(currentTransfers.value.indexOf(promise), 1)
        })
        return promise
    }

    /**
     * Waits until all transfers are completed.
     */
    async function waitForAllTransfersDone() {
        if(currentTransfers.value.length > 0) {
            await Promise.all(currentTransfers.value)
        }
    }

    async function deletePitchReport(pitchReport: Pitch) {
        await waitForAllTransfersDone()
        if (pitchReport.id) {
            await trackTransfer(deletePitchOnServer(pitchReport))
                .catch((error) => {
                    newError(error)
                })
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

    function newError(message: string) {
        currentErrors.value.push(new ErrorMessage(message))
        console.log(message)
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
        currentErrors,
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
        sendPitchReport,
        sendDisciplinaryAction,
        sendInjury,
        waitForAllTransfersDone,
        deletePitchReport,
        newError
    }
})