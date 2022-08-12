import type {ExtraTimeOption, GameReport, GameType, Report, Rule, SingleTeamGameReport} from "@/types";
import {DisciplinaryActionDEO, InjuryDEO} from "@/types";
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
    let gameReportDEO = cGameReportDEO.gameReport;
    console.log(gameReportDEO)
    let gameTypeVal = gameTypes.find(gameType => gameType.id === gameReportDEO.gameType);
    let extraTimeVal = extraTimeOptions.find(extraTime => extraTime.id === gameReportDEO.extraTime);
    let teamAVal = report.selectedTeams.find(team => team.id === gameReportDEO.teamA);
    let teamBVal = report.selectedTeams.find(team => team.id === gameReportDEO.teamB);
    if (teamAVal && teamBVal) {
        let teamAinjuries = cGameReportDEO.injuries
            ?.filter(injury => injury.team === gameReportDEO.teamA)
            ?.map(injury => injuryDEOToInjury(injury, teamAVal));
        let teamAdisciplinaryActions = cGameReportDEO.disciplinaryActions
            ?.filter(disciplinaryAction => disciplinaryAction.team === gameReportDEO.teamA)
            ?.map(disciplinaryAction => fromDisciplinaryActionDEOToDisciplinaryAction(disciplinaryAction, rules, teamAVal));
        let teamBinjuries = cGameReportDEO.injuries
            ?.filter(injury => injury.team === gameReportDEO.teamB)
            ?.map(injury => injuryDEOToInjury(injury, teamBVal));
        let teamBdisciplinaryActions = cGameReportDEO.disciplinaryActions
            ?.filter(disciplinaryAction => disciplinaryAction.team === gameReportDEO.teamB)
            ?.map(disciplinaryAction => fromDisciplinaryActionDEOToDisciplinaryAction(disciplinaryAction, rules, teamBVal));
        let teamAReport = {
            team: teamAVal,
            goals: gameReportDEO.teamAGoals,
            points: gameReportDEO.teamAPoints,
            injuries: teamAinjuries,
            disciplinaryActions: teamAdisciplinaryActions
        } as SingleTeamGameReport
        let teamBReport = {
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

export async function updateGameReport(gameReport: GameReport) {
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
        console.log("Update complete")
    } else {
        console.log(data)
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