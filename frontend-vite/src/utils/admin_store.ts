import {defineStore} from "pinia";
import {ref} from "vue";
import {
    createPitchPropertyOnServer,
    deletePitchPropertyOnServer, enablePitchPropertyOnServer,
    getPitchVariables,
    pitchDEOtoPitch,
    updatePitchPropertyOnServer
} from "@/utils/api/pitch_api";
import type {GameCode} from "@/types";
import {ErrorMessage, ExtraTimeOption, GameType} from "@/types";
import {
    completeReportDEOToReport,
    extractGameReportsFromCompleteReportDEO,
    getGameCodes,
    loadReportDEO
} from "@/utils/api/report_api";
import {getRules} from "@/utils/api/disciplinary_action_api";
import {getGameReportVariables, uploadNewGameType} from "@/utils/api/game_report_api";
import {addRuleOnServer, updateGameTypeOnServer} from "@/utils/api/admin_api";
import type {NewRuleDEO, Rule} from "@/types/rules_types";
import type {PitchProperty, PitchVariables} from "@/types/pitch_types";
import {PitchPropertyType} from "@/types/pitch_types";

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
        if (option.id === -1) {
            createPitchPropertyOnServer(option)
                .then(it => {
                    if (it.id) {
                        option.id = it.id
                    } else {
                        throw new Error("Server did respond with wrong id when creating a new pitch property")
                    }
                })
                .catch(reason => newError(reason))

        } else {
            const prop = getVariablesByType(type).find(it => it.id === option.id)
            if (prop) {
                prop.name = option.name
                updatePitchPropertyOnServer(prop)
                    .then(it => {
                        prop.name = it.name
                        prop.id = it.id ?? -1
                    })
                    .catch(reason => newError(reason))
            }
        }
    }

    async function deletePitchVariable(option: PitchProperty, type: PitchPropertyType) {
        const prop = getVariablesByType(type).find(it => it.id === option.id)
        if (prop) {
            deletePitchPropertyOnServer(prop)
                .then((response) => {
                    if (response.deletionState === "DELETED") {
                        const index = getVariablesByType(type).indexOf(prop)
                        if (index >= 0) {
                            getVariablesByType(type).splice(index, 1)
                        }
                    } else if (response.deletionState == "DISABLED") {
                        prop.disabled = true
                    } else {
                        newError("Could not delete or disable pitch variable")
                    }
                })
                .catch(reason => newError(reason))
        }
    }

    async function enablePitchVariable(option: PitchProperty, type: PitchPropertyType) {
        const prop = getVariablesByType(type).find(it => it.id === option.id)
        if (prop) {
            enablePitchPropertyOnServer(prop)
                .then(() => {
                    prop.disabled = false
                })
                .catch(reason => newError(reason))
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

    async function addRule(rule: NewRuleDEO) {
        try {
            await addRuleOnServer(rule)
            rules.value = await getRules()
        } catch (e: any) {
            newError(e)
        }

    }


    async function updateGameType(gameType: GameType) {
        try {
            let result = await updateGameTypeOnServer(gameType)
            let index = gameTypes.value.findIndex(it => it.id === result.id)
            if (index >= 0) {
                gameTypes.value[index] = result
            }
        } catch (e: any) {
            newError(e)
        }
    }

    async function createGameType(type: GameType) {
        try {
            let result = await uploadNewGameType(type.name)
            gameTypes.value.push(result)
        } catch (e: any) {
            newError(e)
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
        deletePitchVariable,
        enablePitchVariable,
        currentErrors,
        newError,
        findCodeById,
        getCompleteReport,
        updateRuleInStore,
        deleteRuleInStore,
        updateGameType,
        addRule,
        createGameType
    }
})