import {z} from "zod";
import {DateTime} from "luxon";

export const Tournament = z.object({
    name: z.string().min(1),
    location: z.string().min(1),
    date: z.string()
        .transform((value) => DateTime.fromISO(value)),
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

