import type {Report} from "@/types/report_types";
import {CompactTournamentReportDEO} from "@/types/report_types";

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
    generalNotes: string
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
    redCardIssued: z.boolean(),
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
    redCardIssued: boolean,
    forTeamOfficial: boolean,
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
    umpireNotes: z.string(),
    generalNotes: z.string().optional().nullable(),
});
export type GameReportDEO = z.infer<typeof GameReportDEO>;
export const CompleteGameReportDEO = z.object({
    gameReport: GameReportDEO,
    injuries: InjuryDEO.array().nullable(),
    disciplinaryActions: DisciplinaryActionDEO.array().nullable()
})
export type CompleteGameReportDEO = z.infer<typeof CompleteGameReportDEO>;

export const CompleteGameReportWithRefereeReportDEO = z.object({
    gameReport: CompleteGameReportDEO,
    refereeReport: CompactTournamentReportDEO
})
export type CompleteGameReportWithRefereeReportDEO = z.infer<typeof CompleteGameReportWithRefereeReportDEO>;

export const PublicDisciplinaryActionDEO = z.object({
    id: z.number(),
    team: z.number(),
    rule: z.number(),
    game: z.number(),
    redCardIssued: z.boolean()
})
export const PublicGameReportDEO = z.object({
    gameReport: GameReportDEO,
    disciplinaryActions: z.array(PublicDisciplinaryActionDEO),
    code: z.number()
})

export type PublicGameReportDEO = z.infer<typeof PublicGameReportDEO>


export function compareGameReportByStartTime(a: GameReport, b: GameReport) {
    if (a.startTime) {
        if (b.startTime) {
            return a.startTime > b.startTime ? 1 : -1
        } else {
            return -1
        }
    } else {
        return 0
    }
}
