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
import {PitchPropertyType} from "@/types/pitch_types";
import type {Team} from "@/types/team_types";
import {loadAllTeams} from "@/utils/api/teams_api";

export const usePublicStore = defineStore("public",() =>{
    const codes = ref<Array<GameCode>>([])
    const rules = ref<Array<Rule>>([])
    const gameTypes = ref<Array<GameType>>([])
    const extraTimeOptions = ref<Array<ExtraTimeOption>>([])
    const pitchVariables = ref<PitchVariables | undefined>()
    const regions = ref<Array<RegionDEO>>([])
    const tournaments = ref<Array<DatabaseTournament>>([])
    const teams = ref<Array<Team>>([])


    const currentErrors = ref<ErrorMessage[]>([])

    async function loadAuxiliaryInformationFromServer(forceReload = true) {
        const promiseList = []
        if (forceReload || codes.value.length == 0) {
            promiseList.push(getGameCodes()
                .then(serverCodes => codes.value = serverCodes)
                .catch(error => currentErrors.value.push(new ErrorMessage(error)))
            )
        }

        if(forceReload || rules.value.length == 0) {
            promiseList.push(getRules()
                .then(serverRules => rules.value = serverRules)
                .catch(reason => currentErrors.value.push(new ErrorMessage(reason)))
            )
        }

        if(forceReload || pitchVariables.value == undefined) {
            promiseList.push(getPitchVariables()
                .then(serverPitchVariables => pitchVariables.value = serverPitchVariables)
                .catch(reason => currentErrors.value.push(new ErrorMessage(reason)))
            )
        }

        if(forceReload || gameTypes.value.length == 0) {
            promiseList.push(getGameReportVariables()
                .then(gameReportVariables => {
                    gameTypes.value = gameReportVariables.gameTypes
                    extraTimeOptions.value = gameReportVariables.extraTimeOptions
                })
                .catch(reason => currentErrors.value.push(new ErrorMessage(reason)))
            )
        }

        if(forceReload || regions.value.length == 0) {
            promiseList.push(loadAllRegions()
                .then(regionsFromServer => {
                    regions.value = regionsFromServer
                })
                .catch(reason => currentErrors.value.push(new ErrorMessage(reason)))
            )
        }


        if(forceReload || teams.value.length == 0) {
            promiseList.push(loadTeams())
        }

        return Promise.all(promiseList)
    }

    async function loadTeams() {
        return loadAllTeams().then(teamsFromServer => {
            teams.value = teamsFromServer
        })
            .catch(e => newError(e))
    }



    async function loadTournaments() {
        return loadAllTournaments()
            .then(tournamentsFromServer => {
                tournaments.value = tournamentsFromServer
            })
            .catch(e => newError(e))

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

    function findCodeById(id: number) {
        return codes.value.find(it => it.id === id)
    }

    function newError(message: string) {
        currentErrors.value.push(new ErrorMessage(message))
        console.log(message)
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

    function findTeamById(teamID: number):Team | undefined   {
        return teams.value.find((team) => team.id === teamID)
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
        teams,
        loadAuxiliaryInformationFromSerer: loadAuxiliaryInformationFromServer,
        loadTeams,
        newError,
        loadTournaments,
        getVariablesByType,
        waitForAllVariablesPresent,
        findCodeById,
        findTeamById,
    }
});
