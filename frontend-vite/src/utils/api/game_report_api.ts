import type {GameReport, Report, Rule, SingleTeamGameReport} from "@/types";
import {ApiError, DisciplinaryActionDEO, GameType, InjuryDEO, ExtraTimeOption} from "@/types";
import {z} from "zod"
import {DateTime} from "luxon";
import {injuryDEOToInjury} from "@/utils/api/injuries_api";
import {fromDisciplinaryActionDEOToDisciplinaryAction} from "@/utils/api/disciplinary_action_api";

export const GameReportDEO = z.object({
    id: z.number(),
    report: z.number(),
    teamA: z.number(),
    teamB: z.number(),
    teamAGoals: z.number(),
    teamBGoals: z.number(),
    teamAPoints: z.number(),
    teamBPoints: z.number(),
    startTime: z.string().transform((value) => DateTime.fromISO(value)),
    extraTime: z.number().optional().nullable(),
    gameType: z.number().optional().nullable(),
    umpirePresentOnTime: z.boolean(),
    umpireNotes: z.string()
});
export type GameReportDEO = z.infer<typeof GameReportDEO>;


export const CompleteGameReportDEO = z.object({
    gameReport: GameReportDEO,
    injuries: InjuryDEO.array().nullable(),
    disciplinaryActions: DisciplinaryActionDEO.array().nullable()
})
export type CompleteGameReportDEO = z.infer<typeof CompleteGameReportDEO>;

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
    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify(
            gameReportToGameReportDEO(gameReport)
        )
    }
    const response = await fetch("/api/gamereport/update", requestOptions)
    const data = await response.json()
    const parseResult = GameReportDEO.safeParse(data)
    if (parseResult.success) {
        return parseResult.data.id
    } else {
        const errorParse = ApiError.safeParse(data)
        if (errorParse.success) {
            return Promise.reject(errorParse.data)
        }
        return Promise.reject("Error updating game report")
    }

}

export async function createGameReport(gameReport: GameReport): Promise<number> {


    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify(gameReportToGameReportDEO(gameReport))
    };
    const response = await fetch("/api/gamereport/new", requestOptions)
    const data = await response.json()
    const parseResult = GameReportDEO.safeParse(data)
    console.log(data)
    if (parseResult.success) {
        console.log("Game report created")
        return parseResult.data.id
    }
    return -1

}

export async function uploadNewGameType(gameTypeName: string):Promise<GameType> {
    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify({name: gameTypeName})
    };
    const response = await fetch("/api/gametype/new", requestOptions)
    const data = await response.json()
    const parseResult = GameType.safeParse(data)
    if (parseResult.success) {
        console.log("Game type created")
        return parseResult.data
    } else {
        const apiErrorParse = ApiError.safeParse(data)
        if (apiErrorParse.success) {
            return Promise.reject(apiErrorParse.data.message)
        } else {
            return Promise.reject("Unknown error")
        }
    }
}

export async function deleteGameReportOnServer(gameReport: GameReport): Promise<boolean> {
    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify({id: gameReport.id})
    };
    const response = await fetch("/api/gamereport/delete", requestOptions)
    const data = await response.json()
    const parseResult = z.object({
        id: z.number(),
    }).safeParse(data)
    if (parseResult.success) {
        return true
    } else {
        const epR = ApiError.safeParse(data)
        if (epR.success) {
            return Promise.reject(epR.data.message)
        } else {
            return Promise.reject("Unknown error")
        }
    }
}

export async function getGameReportVariables() {
    return fetch("/api/game_report_variables")
        .then(response => response.json())
        .then(data => z.object({
            gameTypes: z.array(GameType),
            extraTimeOptions: z.array(ExtraTimeOption),
        }).safeParse(data))
        .then(parseResult => {
            if (parseResult.success) {
                return parseResult.data
            } else {
                return Promise.reject("Could not load or parse game types")
            }
        })

}