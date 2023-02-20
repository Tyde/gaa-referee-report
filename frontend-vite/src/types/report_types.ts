import {number, z} from "zod";
import {DatabaseTournament} from "@/types/tournament_types";
import {Team} from "@/types/team_types";
import {GameCode} from "@/types";
import {DateTime} from "luxon";
import {Referee} from "@/types/referee_types";
import {PitchDEO} from "@/types/pitch_types";
import {CompleteGameReportDEO} from "@/types/game_report_types";

export type Report = z.infer<typeof Report>
export const Report = z.object({
    id: number().optional().nullable(),
    tournament: DatabaseTournament,
    selectedTeams: Team.array(),
    gameCode: GameCode,
    additionalInformation: z.string().optional(),
    isSubmitted: z.boolean().optional().nullable(),
    submitDate: z.string().optional().nullable().transform((value) => {
        if (value) {
            return DateTime.fromISO(value)
        } else {
            return null
        }
    }),
    referee: Referee
})
export const ReportDEO = z.object({
        id: number(),
        tournament: number(),
        code: number(),
        additionalInformation: z.string().optional(),
        isSubmitted: z.boolean(),
        submitDate: z.string().optional().nullable().transform((value) => {
            if (value) {
                return DateTime.fromISO(value)
            } else {
                return null
            }
        }),
    }
)
export type ReportDEO = z.infer<typeof ReportDEO>
export const CompleteReportDEO = z.object({
    id: z.number(),
    tournament: DatabaseTournament,
    code: z.number(),
    additionalInformation: z.string().optional(),
    isSubmitted: z.boolean(),
    submitDate: z.string().transform((value) => DateTime.fromISO(value)).nullable(),
    selectedTeams: Team.array(),
    gameReports: CompleteGameReportDEO.array(),
    pitches: PitchDEO.array().nullable(),
    referee: Referee
})
export type CompleteReportDEO = z.infer<typeof CompleteReportDEO>;
export const CompactTournamentReportDEO = z.object({
    id: z.number(),
    tournament: z.number(),
    code: z.number(),
    isSubmitted: z.boolean(),
    submitDate: z.string().transform((value) => DateTime.fromISO(value)).optional().nullable(),
    refereeId: z.number(),
    refereeName: z.string(),
    numGameReports: z.number(),
    numTeams: z.number(),
})
export type CompactTournamentReportDEO = z.infer<typeof CompactTournamentReportDEO>;
export const NewReportDEO = z.object({
    id: z.number().nullable().optional(),
    tournament: number(),
    gameCode: number(),
    selectedTeams: number().array(),
});
export const UpdateReportAdditionalInformationDEO = z.object({
    id: z.number(),
    additionalInformation: z.string()
})
export type UpdateReportAdditionalInformationDEO = z.infer<typeof UpdateReportAdditionalInformationDEO>;

export const TournamentReportShareLinkDEO = z.object({
    id: z.number(),
    uuid: z.string(),
})
export type TournamentReportShareLinkDEO = z.infer<typeof TournamentReportShareLinkDEO>;