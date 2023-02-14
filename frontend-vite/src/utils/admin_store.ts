import {defineStore} from "pinia";
import {ref} from "vue";
import type {PitchVariables} from "@/utils/api/pitch_api";
import {getPitchVariables, pitchDEOtoPitch, updatePitchPropertyOnServer} from "@/utils/api/pitch_api";
import type {GameCode, PitchProperty, Rule} from "@/types";
import {ErrorMessage, ExtraTimeOption, GameType, PitchPropertyType, Tournament} from "@/types";
import {
    completeReportDEOToReport,
    extractGameReportsFromCompleteReportDEO,
    getGameCodes,
    loadReportDEO
} from "@/utils/api/report_api";
import {getRules} from "@/utils/api/disciplinary_action_api";
import {getGameReportVariables} from "@/utils/api/game_report_api";

export const useAdminStore = defineStore('admin', () => {
    const pitchVariables = ref<PitchVariables>()
    const gameTypes = ref<Array<GameType>>([])
    const extraTimeOptions = ref<Array<ExtraTimeOption>>([])
    const rules = ref<Array<Rule>>([])
    const codes = ref<Array<GameCode>>([])


    const currentErrors = ref<ErrorMessage[]>([])

    function fetchGameReportVariables() {
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

    function fetchPitchVariables() {
        getPitchVariables()
            .then(it => pitchVariables.value = it)
            .catch(
                (error) => {
                    newError(error)
                }
            )
    }

    function getVariablesByType(type: PitchPropertyType) {
        switch (type) {
            case PitchPropertyType.goalDimensions:
                return pitchVariables.value?.goalDimensions || []
            case PitchPropertyType.goalPosts:
                return pitchVariables.value?.goalPosts || []
            case PitchPropertyType.length:
                return pitchVariables.value?.lengths || []
            case PitchPropertyType.width:
                return pitchVariables.value?.widths || []
            case PitchPropertyType.markingsOptions:
                return pitchVariables.value?.markingsOptions || []
            case PitchPropertyType.surface:
                return pitchVariables.value?.surfaces || []
        }
        return []
    }

    async function updatePitchVariable(option: PitchProperty, type: PitchPropertyType) {
        const prop = getVariablesByType(type).find(it => it.id === option.id)
        if (prop) {
            prop.name = option.name
            await updatePitchPropertyOnServer(prop)
        }
    }

    function newError(message: string) {
        currentErrors.value.push(new ErrorMessage(message))
    }

    function findCodeById(id: number) {
        return codes.value.find(it => it.id === id)
    }

    async function waitForAllVariablesPresent() {
        const start_time = new Date().getTime()
        while (true) {
            if (
                codes.value.length > 0 &&
                rules.value.length > 0 &&
                gameTypes.value.length > 0 &&
                extraTimeOptions.value.length > 0 &&
                pitchVariables.value
            ) {
                break
            }
            if ((new Date()).getTime() > start_time + 3000) {
                newError("Timeout waiting for variables")
            }
            await new Promise(resolve => setTimeout(resolve, 500));
        }
    }

    async function getCompleteReport(reportId: number) {
        const deo = await loadReportDEO(reportId)
        await waitForAllVariablesPresent()
        let currentReport = completeReportDEOToReport(deo, codes.value)
        let allGameReports = extractGameReportsFromCompleteReportDEO(
            deo,
            currentReport,
            gameTypes.value,
            extraTimeOptions.value,
            rules.value
        )
        let allPitchReports = deo.pitches?.map(it => pitchDEOtoPitch(it, currentReport, pitchVariables.value!!)) ?? []
        return {
            report: currentReport,
            gameReports: allGameReports,
            pitchReports: allPitchReports
        }
    }

    async function updateRuleInStore(rule: Rule) {
        const index = rules.value.findIndex(it => it.id === rule.id)
        if (index >= 0) {
            rules.value[index] = rule
        }
    }

    async function deleteRuleInStore(ruleId: number) {
        const index = rules.value.findIndex(it => it.id === ruleId)
        if (index >= 0) {
            rules.value.splice(index, 1)
        }
    }


    return {
        pitchVariables,
        gameTypes,
        extraTimeOptions,
        rules,
        codes,
        fetchGameReportVariables,
        fetchPitchVariables,
        getVariablesByType,
        updatePitchVariable,
        currentErrors,
        newError,
        findCodeById,
        getCompleteReport,
        updateRuleInStore,
        deleteRuleInStore
    }
})