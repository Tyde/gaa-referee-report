import {number, z} from "zod";
import {DatabaseTournament} from "@/types/tournament_types";
import {Team} from "@/types/team_types";
import {GameCode} from "@/types";
import {DateTime} from "luxon";
import {Referee} from "@/types/referee_types";

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