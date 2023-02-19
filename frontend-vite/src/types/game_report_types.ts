import type {Report} from "@/types/report_types";

import {DateTime} from "luxon";
import {z} from "zod";
import type {Team} from "@/types/team_types";
import type {ExtraTimeOption, GameType} from "@/types";
import type {Rule} from "@/types/rules_types";

export interface GameReport {
    id?: number
    report: Report,
    startTime?: DateTime,
    gameType?: GameType,
    teamAReport: SingleTeamGameReport,
    teamBReport: SingleTeamGameReport,
    extraTime?: ExtraTimeOption,
    umpirePresentOnTime: boolean,
    umpireNotes: string
}

export const InjuryDEO = z.object({
    id: z.number(),
    firstName: z.string(),
    lastName: z.string(),
    details: z.string(),
    team: z.number()
})
export type InjuryDEO = z.infer<typeof InjuryDEO>
export const DisciplinaryActionDEO = z.object({
    id: z.number(),
    team: z.number(),
    firstName: z.string(),
    lastName: z.string(),
    number: z.number(),
    rule: z.number(),
    details: z.string(),
})
export type DisciplinaryActionDEO = z.infer<typeof DisciplinaryActionDEO>

export interface DisciplinaryAction {
    id?: number,
    team?: Team,
    firstName: string,
    lastName: string,
    number?: number,
    rule?: Rule,
    details: string,
}

export interface Injury {
    id?: number,
    firstName: string,
    lastName: string,
    details: string,
    team?: Team,
}

export interface SingleTeamGameReport {
    team?: Team,
    goals?: number,
    points?: number,
    injuries: Injury[],
    disciplinaryActions: DisciplinaryAction[],
}

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