import {z} from "zod";

export const Rule = z.object({
    id: z.number(),
    code: z.number(),
    isCaution: z.boolean(),
    isBlack: z.boolean(),
    isRed: z.boolean(),
    description: z.string(),
    isDisabled: z.boolean(),
})
export type Rule = z.infer<typeof Rule>
export const NewRuleDEO = Rule.omit({id: true})
export type NewRuleDEO = z.infer<typeof NewRuleDEO>