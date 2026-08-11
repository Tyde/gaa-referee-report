import {z} from "zod";

export interface TeamAliasDEO {
    id: number,
    teamId: number,
    alias: string,
}

export const TeamAliasDEO: z.ZodType<TeamAliasDEO> = z.object({
    id: z.number(),
    teamId: z.number(),
    alias: z.string().min(1),
})

export interface Team {
    name: string,
    id: number,
    isAmalgamation: boolean,
    amalgamationTeams?: Team[] | null,
    changeDate?: string | null,
    aliases?: TeamAliasDEO[] | null,
}

export const Team: z.ZodType<Team> = z.lazy(() =>
    z.object({
        name: z.string().min(1),
        id: z.number(),
        isAmalgamation: z.boolean(),
        amalgamationTeams: Team.array().optional().nullable(),
        changeDate: z.string().optional().nullable(),
        aliases: TeamAliasDEO.array().optional().nullable()
    })
);
export const NewTeamDEO = z.object({
    name: z.string().min(1),
    changeDate: z.string().optional().nullable(),
})
export type NewTeamDEO = z.infer<typeof NewTeamDEO>;

export const MergeTeamsDEO = z.object({
    baseTeam: z.number(),
    teamsToMerge: z.array(z.number()),
    changeDate: z.string().optional().nullable(),
    aliasesToCreate: z.record(z.string(), z.string()).optional(),
})

export type MergeTeamsDEO = z.infer<typeof MergeTeamsDEO>;

export interface TeamHistoryEventDEO {
    changeType: string,
    changeDate: string,
    oldValue?: string | null,
    newValue?: string | null,
    recordedAt: string,
}

export const TeamHistoryEventDEO: z.ZodType<TeamHistoryEventDEO> = z.object({
    changeType: z.string(),
    changeDate: z.string(),
    oldValue: z.string().nullable().optional(),
    newValue: z.string().nullable().optional(),
    recordedAt: z.string(),
})
