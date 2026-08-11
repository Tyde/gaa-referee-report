import {z} from "zod";

export const NewApiTokenDEO = z.object({
    name: z.string(),
    expiresInDays: z.number().int().nullable().optional()
})
export type NewApiTokenDEO = z.infer<typeof NewApiTokenDEO>

export const ApiTokenCreatedDEO = z.object({
    id: z.number(),
    name: z.string(),
    token: z.string(),
    expiresAt: z.string().nullable()
})
export type ApiTokenCreatedDEO = z.infer<typeof ApiTokenCreatedDEO>

export const ApiTokenDEO = z.object({
    id: z.number(),
    name: z.string(),
    createdAt: z.string(),
    expiresAt: z.string().nullable(),
    revoked: z.boolean(),
    lastUsedAt: z.string().nullable()
})
export type ApiTokenDEO = z.infer<typeof ApiTokenDEO>
