import {z} from "zod";
import {DatabaseTournament} from "@/types/tournament_types";
import {DateTime} from "luxon";
import {Team} from "@/types/team_types";
import {CompleteGameReportDEO} from "@/types/game_report_types";
import {PitchDEO} from "@/types/pitch_types";
import {Referee} from "@/types/referee_types";

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
