import {defineStore} from "pinia";
import {computed, ref} from "vue";
import type {ExtraTimeOption, GameCode, GameType} from "@/types";
import {ErrorMessage} from "@/types";
import {getRules, uploadDisciplinaryAction} from "@/utils/api/disciplinary_action_api";
import {
    completeReportDEOToReport,
    extractGameReportsFromCompleteReportDEO,
    getGameCodes,
    loadReportDEO
} from "@/utils/api/report_api";
import {
    checkPitchReadForUpload,
    deletePitchOnServer,
    getPitchVariables,
    pitchDEOtoPitch,
    uploadPitch
} from "@/utils/api/pitch_api";
import {
    createGameReport,
    deleteGameReportOnServer,
    getGameReportVariables,
    updateGameReport
} from "@/utils/api/game_report_api";
import {checkGameReportMinimal} from "@/utils/gobal_functions";
import {uploadInjury} from "@/utils/api/injuries_api";
import type {Report} from "@/types/report_types";
import type {DisciplinaryAction, GameReport, Injury} from "@/types/game_report_types";
import type {Rule} from "@/types/rules_types";
import type {Pitch, PitchVariables} from "@/types/pitch_types";
import {loadAllRegions, loadTournamentPreselectedTeams} from "@/utils/api/tournament_api";
import {RegionDEO} from "@/types/tournament_types";
import {
    disciplinaryActionIssuesForGameReport,
    GameReportIssue,
    GameReportIssues,
    injuryIssuesForGameReport,
    PitchReportIssue
} from "@/types/issue_types";
import type {Team} from "@/types/team_types";
import {loadAllTeams} from "@/utils/api/teams_api";
import {usePublicStore} from "@/utils/public_store";
import {compileAsync} from "sass";
import {computedAsync} from "@vueuse/core";


export enum ReportEditStage {
    SelectTournament,
    SelectTeams,
    EditGameReports,
    EditPitchReports,
    Submit
}
export const useReportStore = defineStore('report', () => {
    const report = ref<Report>({} as Report)
    report.value.selectedTeams = []

    const publicStore = usePublicStore()

    const gameReports = ref<Array<GameReport>>([])
    const pitchReports = ref<Array<Pitch>>([])

    const currentErrors = ref<ErrorMessage[]>([])


    const tournamentPreSelectedTeams = computedAsync(async () => {
        if (report.value.tournament) {
            try {
                const teams =
                    await loadTournamentPreselectedTeams(report.value.tournament.id)
                return teams.teamIds.map((teamId) => {
                    return publicStore.findTeamById(teamId)
                }).filter((team) : team is Team => !!team) as Team[]
            } catch (e:any) {
                newError(e)
                return [] as Team[]
            }
        }
    },
        [] as Team[]
    )


    const enabledPitchVariables = computed(() => {
        if (publicStore.pitchVariables != undefined) {
            let local = {
                ... publicStore.pitchVariables,
            } as PitchVariables
            local.goalDimensions = local.goalDimensions.filter(it => !it.disabled)
            local.goalPosts = local.goalPosts.filter(it => !it.disabled)
            local.lengths = local.lengths.filter(it => !it.disabled)
            local.widths = local.widths.filter(it => !it.disabled)
            local.markingsOptions = local.markingsOptions.filter(it => !it.disabled)
            local.surfaces = local.surfaces.filter(it => !it.disabled)
            return local

        } else {
            return undefined
        }
    })

    const selectedGameReportIndex = ref<number>(-1)
    const selectedGameReport = computed(() => {
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


    function findSquadsForTeam(team:Team) {
        return publicStore.teams.filter((fiTeam) => {
            if (fiTeam.isAmalgamation &&
                fiTeam.amalgamationTeams &&
                fiTeam.amalgamationTeams.length==1 &&
                fiTeam.amalgamationTeams[0].id == team.id
            ) {
                return true
            }
            return  false
        }).sort((a, b) => {
            return a.name.localeCompare(b.name)
        })
    }

    async function loadReport(id: number) {
        try {
            const reportDEOPromise = loadReportDEO(id)
                .catch((error) => {
                    newError(error)
                    return undefined
                })
            await publicStore.loadAuxiliaryInformationFromSerer()
            const reportDEO = await reportDEOPromise
            if (reportDEO) {
                report.value = completeReportDEOToReport(reportDEO, publicStore.codes)
                gameReports.value = extractGameReportsFromCompleteReportDEO(
                    reportDEO,
                    report.value,
                    publicStore.gameTypes,
                    publicStore.extraTimeOptions,
                    publicStore.rules,
                    publicStore.teams
                )
                if (publicStore.pitchVariables) {
                    pitchReports.value = reportDEO.pitches?.map(pitch => {
                        return pitchDEOtoPitch(pitch, report.value, publicStore.pitchVariables!!)
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
        publicStore.gameTypes.push(gameType)
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
    async function sendGameReport(gameReport: GameReport, allowAsync: boolean = false, throwIfNotReady: boolean = false) {

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
        } else {
            if(throwIfNotReady) {
                throw new Error(`Game report "id: ${gameReport.id}" not ready for upload`)
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
    async function sendPitchReport(pitchReport: Pitch, allowAsync: boolean = false, throwIfNotReady: boolean = false) {
        //Warning! Only use allowAsync if you already checked before that
        // all transfers are completed.
        if(!allowAsync) await waitForAllTransfersDone()
        if(checkPitchReadForUpload(pitchReport)) {

            return trackTransfer(
                uploadPitch(pitchReport)
            )
        } else {
            if(throwIfNotReady) {
                throw new Error(`Pitch "name: ${pitchReport.name},id: ${pitchReport.id}" not ready for upload`)
            }
        }
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


    const pitchReportIssues = computed(() => {
        return pitchReports.value.map((pitch) => {
            let issues: Array<PitchReportIssue> = []
            if (pitch.name.trim().length == 0) {
                issues.push(PitchReportIssue.NoName)
            }
            if (pitch.surface == undefined) {
                issues.push(PitchReportIssue.NoSurface)
            }
            if (pitch.length == undefined || pitch.width == undefined) {
                issues.push(PitchReportIssue.MissingDimension)
            }
            if (pitch.smallSquareMarkings == undefined ||
                pitch.penaltySquareMarkings == undefined ||
                pitch.thirteenMeterMarkings == undefined ||
                pitch.twentyMeterMarkings == undefined ||
                pitch.longMeterMarkings == undefined
            ) {
                issues.push(PitchReportIssue.MarkingsIncomplete)
            }
            if (pitch.goalPosts == undefined ||
                pitch.goalDimensions == undefined
            ) {
                issues.push(PitchReportIssue.GoalInfoIncomplete)
            }
            return ([pitch, issues] as [Pitch, Array<PitchReportIssue>])
        }).filter(([pitch, issues]) => issues.length > 0)
    })

    const gameReportIssues = computed(() => {
        return gameReports.value.map((gameReport) => {
            let issues: Array<GameReportIssue> = []
            if (gameReport.gameType == undefined) {
                issues.push(GameReportIssue.NoGameType)
            }
            if (gameReport.startTime == undefined) {
                issues.push(GameReportIssue.NoStartingTime)
            }
            if (gameReport.extraTime == undefined) {
                issues.push(GameReportIssue.NoExtraTimeOption)
            }
            if (gameReport.teamAReport.team == undefined) {
                issues.push(GameReportIssue.NoTeamA)
            }
            if (gameReport.teamBReport.team == undefined) {
                issues.push(GameReportIssue.NoTeamB)
            }
            if ((gameReport.teamAReport?.goals || 0) +
                (gameReport.teamBReport?.goals || 0) +
                (gameReport.teamAReport?.points || 0) +
                (gameReport.teamBReport?.points || 0) == 0) {
                issues.push(GameReportIssue.NoScores)
            }
            if(gameReport.teamAReport.team == gameReport.teamBReport.team) {
                issues.push(GameReportIssue.TeamAEqualTeamB)
            }
            let daIssues = disciplinaryActionIssuesForGameReport(gameReport)
            let inIssues = injuryIssuesForGameReport(gameReport)


            return new GameReportIssues(gameReport, issues, daIssues, inIssues)
        }).filter((gris) => (gris.issues.length +
            gris.injuriesIssues.length +
            gris.disciplinaryActionIssues.length) > 0)
    })
    async function uploadDataToServerBeforeLeaving() {
        await waitForAllTransfersDone()
        const promises:Promise<any>[] = []

        if (selectedPitchReport.value) {
            promises.push(sendPitchReport(selectedPitchReport.value, true,true))
        }
        if (selectedGameReport.value) {
            promises.push(sendGameReport(selectedGameReport.value, true, true))
        }
        return Promise.all(promises)
    }

    return {
        publicStore,
        report,
        tournamentPreSelectedTeams,
        gameReports,
        pitchReports,
        //codes,
        //rules,
        //gameTypes,
        //extraTimeOptions,
        //pitchVariables,
        //regions,
        enabledPitchVariables,
        currentErrors,
        selectedGameReportIndex,
        selectedGameReport,
        selectedGameReportPassesMinimalRequirements,
        selectedPitchReportIndex,
        selectedPitchReport,
        //allTeams,
        findSquadsForTeam,
        //loadAuxiliaryInformationFromSerer,
        //loadAllTeamsFromServer,
        loadReport,
        addNewGameType,
        deleteGameReport,
        sendGameReport,
        sendPitchReport,
        sendDisciplinaryAction,
        sendInjury,
        waitForAllTransfersDone,
        deletePitchReport,
        newError,
        uploadDataToServerBeforeLeaving,
        pitchReportIssues,
        gameReportIssues
    }
})
