import {defineStore} from "pinia";

import {ErrorMessage, ExtraTimeOption, GameCode, GameType} from "@/types";
import {Rule} from "@/types/rules_types";
import type {PitchVariables} from "@/types/pitch_types";
import {DatabaseTournament, RegionDEO} from "@/types/tournament_types";
import {getGameCodes} from "@/utils/api/report_api";
import {getRules} from "@/utils/api/disciplinary_action_api";
import {getPitchVariables} from "@/utils/api/pitch_api";
import {getGameReportVariables} from "@/utils/api/game_report_api";
import {loadAllRegions, loadAllTournaments} from "@/utils/api/tournament_api";
import {ref} from "vue";

export const usePublicStore = defineStore("public",() =>{
    const codes = ref<Array<GameCode>>([])
    const rules = ref<Array<Rule>>([])
    const gameTypes = ref<Array<GameType>>([])
    const extraTimeOptions = ref<Array<ExtraTimeOption>>([])
    const pitchVariables = ref<PitchVariables | undefined>()
    const regions = ref<Array<RegionDEO>>([])
    const tournaments = ref<Array<DatabaseTournament>>([])


    const currentErrors = ref<ErrorMessage[]>([])


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

        const promiseRegions = loadAllRegions()
            .then(regionsFromServer => {
                regions.value = regionsFromServer
            })
            .catch(reason => currentErrors.value.push(new ErrorMessage(reason)))

        return Promise.all([promiseCodes, promiseRules, promisePitchVariables, promiseGameReport, promiseRegions])
    }

    async function loadTournaments() {
        return loadAllTournaments()
            .then(tournamentsFromServer => {
                tournaments.value = tournamentsFromServer
            })
            .catch(e => newError(e))

    }

    function newError(message: string) {
        currentErrors.value.push(new ErrorMessage(message))
        console.log(message)
    }

    return {
        codes,
        rules,
        gameTypes,
        extraTimeOptions,
        pitchVariables,
        regions,
        tournaments,
        currentErrors,
        loadAuxiliaryInformationFromSerer,
        newError,
        loadTournaments
    }
});