import {defineStore} from "pinia";
import {ref} from "vue";
import {
    createPitchPropertyOnServer,
    deletePitchPropertyOnServer,
    enablePitchPropertyOnServer,
    pitchDEOtoPitch,
    updatePitchPropertyOnServer
} from "@/utils/api/pitch_api";
import {ErrorMessage, GameType} from "@/types";
import {
    completeReportDEOToReport,
    extractGameReportsFromCompleteReportDEO,
    loadReportDEO
} from "@/utils/api/report_api";
import {getRules} from "@/utils/api/disciplinary_action_api";
import {uploadNewGameType} from "@/utils/api/game_report_api";
import {
    addRuleOnServer,
    deleteTournamentOnServer,
    mergeTournamentOnServer,
    updateGameTypeOnServer
} from "@/utils/api/admin_api";
import type {NewRuleDEO, Rule} from "@/types/rules_types";
import type {PitchProperty} from "@/types/pitch_types";
import {PitchPropertyType} from "@/types/pitch_types";
import {usePublicStore} from "@/utils/public_store";
import type {DatabaseTournament} from "@/types/tournament_types";

export const useAdminStore = defineStore('admin', () => {
    const publicStore = usePublicStore()

    const currentErrors = ref<ErrorMessage[]>([])








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
            const prop = publicStore.getVariablesByType(type).find(it => it.id === option.id)
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
        const prop = publicStore.getVariablesByType(type).find(it => it.id === option.id)
        if (prop) {
            deletePitchPropertyOnServer(prop)
                .then((response) => {
                    if (response.deletionState === "DELETED") {
                        const index = publicStore.getVariablesByType(type).indexOf(prop)
                        if (index >= 0) {
                            publicStore.getVariablesByType(type).splice(index, 1)
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
        const prop = publicStore.getVariablesByType(type).find(it => it.id === option.id)
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





    async function getCompleteReport(reportId: number) {
        const deo = await loadReportDEO(reportId)
        await publicStore.waitForAllVariablesPresent()
        let currentReport = completeReportDEOToReport(deo, publicStore.codes)
        let allGameReports = extractGameReportsFromCompleteReportDEO(
            deo,
            currentReport,
            publicStore.gameTypes,
            publicStore.extraTimeOptions,
            publicStore.rules,
            publicStore.teams
        )
        let allPitchReports = deo.pitches
            ?.map(it => pitchDEOtoPitch(it, currentReport, publicStore.pitchVariables!!)) ?? []
        return {
            report: currentReport,
            gameReports: allGameReports,
            pitchReports: allPitchReports
        }
    }

    async function updateRuleInStore(rule: Rule) {
        const index = publicStore.rules.findIndex(it => it.id === rule.id)
        if (index >= 0) {
            publicStore.rules[index] = rule
        }
    }

    async function deleteRuleInStore(ruleId: number) {
        const index = publicStore.rules.findIndex(it => it.id === ruleId)
        if (index >= 0) {
            publicStore.rules.splice(index, 1)
        }
    }

    async function addRule(rule: NewRuleDEO) {
        try {
            await addRuleOnServer(rule)
            publicStore.rules = await getRules()
        } catch (e: any) {
            newError(e)
        }

    }


    async function updateGameType(gameType: GameType) {
        try {
            let result = await updateGameTypeOnServer(gameType)
            let index = publicStore.gameTypes.findIndex(it => it.id === result.id)
            if (index >= 0) {
                publicStore.gameTypes[index] = result
            }
        } catch (e: any) {
            newError(e)
        }
    }

    async function createGameType(type: GameType) {
        try {
            let result = await uploadNewGameType(type.name)
            publicStore.gameTypes.push(result)
        } catch (e: any) {
            newError(e)
        }
    }

    async function deleteTournament(tournament: DatabaseTournament) {
        deleteTournamentOnServer(tournament)
            .catch(e=>newError(e))
            .then((res) => {
                if(res) {
                    publicStore.loadTournaments()
                }
            })
    }

    async function mergeTournament(from: DatabaseTournament, to: DatabaseTournament) {
        mergeTournamentOnServer(from, to)
            .catch(e=>newError(e))
            .then((res) => {
                if(res) {
                    publicStore.loadTournaments()
                }
            })
    }

    return {
        publicStore,
        updatePitchVariable,
        deletePitchVariable,
        enablePitchVariable,
        currentErrors,
        newError,
        getCompleteReport,
        updateRuleInStore,
        deleteRuleInStore,
        updateGameType,
        addRule,
        createGameType,
        deleteTournament,
        mergeTournament
    }
})
