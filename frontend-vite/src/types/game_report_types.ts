import type {Report} from "@/types/report_types";
import type {DateTime} from "luxon";
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