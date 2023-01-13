import {defineStore} from "pinia";
import {ref} from "vue";
import type {PitchVariables} from "@/utils/api/pitch_api";
import {getPitchVariables, updatePitchPropertyOnServer} from "@/utils/api/pitch_api";
import type {GameCode, PitchProperty, Rule} from "@/types";
import {ErrorMessage, ExtraTimeOption, GameType, PitchPropertyType} from "@/types";
import {getGameCodes} from "@/utils/api/report_api";
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

        return Promise.all([ promiseCodes,promiseRules, promisePitchVariables, promiseGameReport])

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
    }
})