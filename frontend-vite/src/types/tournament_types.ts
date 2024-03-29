import {z} from "zod";
import {DateTime} from "luxon";

export const Tournament = z.object({
    name: z.string().min(1),
    location: z.string().min(1),
    date: z.string()
        .transform((value) => DateTime.fromISO(value)),
    region: z.number(),
    isLeague: z.boolean().optional(),
    endDate: z.string().optional()
        .transform((value) => {
            if(!value) return null
            return DateTime.fromISO(value!!)
        }),
})
export type Tournament = z.infer<typeof Tournament>
export const DatabaseTournament = Tournament.extend({
    id: z.number()
})
export type DatabaseTournament = z.infer<typeof DatabaseTournament>


export function databaseTournamentToTournamentDAO(tournament: DatabaseTournament) {
    return {
        ...tournamentToTournamentDAO(tournament),
        id: tournament.id
    }
}

export function tournamentToTournamentDAO(tournament: Tournament) {
    return {
        name: tournament.name,
        location: tournament.location,
        date: tournament.date.toISODate(),
        region: tournament.region,
        isLeague: tournament.isLeague,
        endDate: tournament.endDate?.toISODate() ?? undefined
    }
}


export const RegionDEO = z.object({
    id: z.number(),
    name: z.string().min(1),
})

export type RegionDEO = z.infer<typeof RegionDEO>

