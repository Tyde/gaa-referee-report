import {z} from "zod";

export interface Team {
    name: string,
    id: number,
    isAmalgamation: boolean,
    amalgamationTeams?: Team[] | null,
}

export const Team: z.ZodType<Team> = z.lazy(() =>
    z.object({
        name: z.string().min(1),
        id: z.number(),
        isAmalgamation: z.boolean(),
        amalgamationTeams: Team.array().optional().nullable()
    })
);
export const NewTeamDEO = z.object({
    name: z.string().min(1),
})
export type NewTeamDEO = z.infer<typeof NewTeamDEO>;