import {z} from "zod";
import {CompleteGameReportWithRefereeReportDEO, PublicGameReportDEO} from "@/types/game_report_types";
import {Team} from "@/types/team_types";
import {DatabaseTournament} from "@/types/tournament_types";

export const CompleteTournamentReportDEO = z.lazy(() =>
    z.object({
        tournament: DatabaseTournament,
        games: CompleteGameReportWithRefereeReportDEO.array(),
        teams: Team.array()
    })
)

export type CompleteTournamentReportDEO = z.infer<typeof CompleteTournamentReportDEO>


export const PublicTournamentReportDEO = z.lazy(() =>
    z.object({
        tournament: DatabaseTournament,
        games: z.array(PublicGameReportDEO),
        teams: z.array(Team)
    })
)

export type PublicTournamentReportDEO = z.infer<typeof PublicTournamentReportDEO>
