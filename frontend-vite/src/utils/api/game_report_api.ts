import {ExtraTimeOption, GameType} from "@/types";
import {z} from "zod"
import {injuryDEOToInjury} from "@/utils/api/injuries_api";
import {fromDisciplinaryActionDEOToDisciplinaryAction} from "@/utils/api/disciplinary_action_api";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import type {Report} from "@/types/report_types";
import type {CompleteGameReportDEO, GameReport, SingleTeamGameReport} from "@/types/game_report_types";
import {GameReportDEO} from "@/types/game_report_types";
import type {Rule} from "@/types/rules_types";


export function gameReportDEOToGameReport(
    cGameReportDEO: CompleteGameReportDEO,
    report: Report,
    gameTypes: Array<GameType>,
    extraTimeOptions: Array<ExtraTimeOption>,
    rules: Array<Rule>
): GameReport | undefined {
    const gameReportDEO = cGameReportDEO.gameReport;
    const gameTypeVal = gameTypes.find(gameType => gameType.id === gameReportDEO.gameType);
    const extraTimeVal = extraTimeOptions.find(extraTime => extraTime.id === gameReportDEO.extraTime);
    const teamAVal = report.selectedTeams.find(team => team.id === gameReportDEO.teamA);
    const teamBVal = report.selectedTeams.find(team => team.id === gameReportDEO.teamB);
    if (teamAVal != undefined && teamBVal != undefined) {
        const teamAinjuries = cGameReportDEO.injuries
            ?.filter(injury => injury.team === gameReportDEO.teamA)
            ?.map(injury => injuryDEOToInjury(injury, teamAVal));
        const teamAdisciplinaryActions = cGameReportDEO.disciplinaryActions
            ?.filter(disciplinaryAction => disciplinaryAction.team === gameReportDEO.teamA)
            ?.map(disciplinaryAction => fromDisciplinaryActionDEOToDisciplinaryAction(disciplinaryAction, rules, teamAVal));
        const teamBinjuries = cGameReportDEO.injuries
            ?.filter(injury => injury.team === gameReportDEO.teamB)
            ?.map(injury => injuryDEOToInjury(injury, teamBVal));
        const teamBdisciplinaryActions = cGameReportDEO.disciplinaryActions
            ?.filter(disciplinaryAction => disciplinaryAction.team === gameReportDEO.teamB)
            ?.map(disciplinaryAction => fromDisciplinaryActionDEOToDisciplinaryAction(disciplinaryAction, rules, teamBVal));
        const teamAReport = {
            team: teamAVal,
            goals: gameReportDEO.teamAGoals,
            points: gameReportDEO.teamAPoints,
            injuries: teamAinjuries,
            disciplinaryActions: teamAdisciplinaryActions
        } as SingleTeamGameReport
        const teamBReport = {
            team: teamBVal,
            goals: gameReportDEO.teamBGoals,
            points: gameReportDEO.teamBPoints,
            injuries: teamBinjuries,
            disciplinaryActions: teamBdisciplinaryActions
        } as SingleTeamGameReport
        return {
            id: gameReportDEO.id,
            report: report,
            startTime: gameReportDEO.startTime,
            gameType: gameTypeVal,
            teamAReport: teamAReport,
            teamBReport: teamBReport,
            extraTime: extraTimeVal,
            umpirePresentOnTime: gameReportDEO.umpirePresentOnTime,
            umpireNotes: gameReportDEO.umpireNotes,
        } as GameReport
    } else {
        return undefined
    }
}


function gameReportToGameReportDEO(gameReport: GameReport) {
    return {
        "id": gameReport.id,
        "report": gameReport.report.id,
        "teamA": gameReport.teamAReport.team?.id,
        "teamB": gameReport.teamBReport.team?.id,
        "teamAGoals": gameReport.teamAReport.goals,
        "teamAPoints": gameReport.teamAReport.points,
        "teamBGoals": gameReport.teamBReport.goals,
        "teamBPoints": gameReport.teamBReport.points,
        "startTime": gameReport.startTime?.toISO(),
        "extraTime": gameReport.extraTime?.id,
        "gameType": gameReport.gameType?.id,
        "umpirePresentOnTime": gameReport.umpirePresentOnTime,
        "umpireNotes": gameReport.umpireNotes,
    }
}



export async function updateGameReport(gameReport: GameReport): Promise<number> {
    return makePostRequest(
        "/api/gamereport/update",
        gameReportToGameReportDEO(gameReport)
    )
        .then(data => parseAndHandleDEO(data, GameReportDEO))
        .then(gameReportDEO => gameReportDEO.id)
}

export async function createGameReport(gameReport: GameReport): Promise<number> {
    return makePostRequest(
        "/api/gamereport/new",
        gameReportToGameReportDEO(gameReport)
    )
        .then(data => parseAndHandleDEO(data, GameReportDEO))
        .then(gameReportDEO => gameReportDEO.id)

}

export async function uploadNewGameType(gameTypeName: string):Promise<GameType> {
    return makePostRequest(
        "/api/gametype/new",
        {name: gameTypeName}
    )
        .then(data => parseAndHandleDEO(data, GameType))
}

export async function deleteGameReportOnServer(gameReport: GameReport): Promise<boolean> {
    return makePostRequest(
        "/api/gamereport/delete",
        {id: gameReport.id}
    )
        .then(data => parseAndHandleDEO(data, z.object({id: z.number(),})))
        .then(() => {
            gameReport.id = -1
            return true
        })
}

export async function getGameReportVariables() {

    return fetch("/api/game_report_variables")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, z.object({
            gameTypes: z.array(GameType),
            extraTimeOptions: z.array(ExtraTimeOption),
        })))
}
