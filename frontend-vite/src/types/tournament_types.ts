import {z} from "zod";
import {DateTime} from "luxon";
import {PublicGameReportDEO} from "@/types/game_report_types";
import {Team} from "@/types/team_types";

export const Tournament = z.object({
    name: z.string().min(1),
    location: z.string().min(1),
    date: z.string().transform((value) => DateTime.fromISO(value)),
    region: z.number(),
})
export type Tournament = z.infer<typeof Tournament>
export const DatabaseTournament = Tournament.extend({
    id: z.number()
})
export type DatabaseTournament = z.infer<typeof DatabaseTournament>



export function databaseTournamentToTournamentDAO(tournament: DatabaseTournament) {
    return {
        name: tournament.name,
        location: tournament.location,
        date: tournament.date.toISODate(),
        region: tournament.region,
        id: tournament.id
    }
}


export const RegionDEO = z.object({
    id: z.number(),
    name: z.string().min(1),
})

export type RegionDEO = z.infer<typeof RegionDEO>


export const PublicTournamentReportDEO = z.object({
    tournament: Tournament,
    games: z.array(PublicGameReportDEO),
    teams: z.array(Team)
})

export type PublicTournamentReportDEO = z.infer<typeof PublicTournamentReportDEO>