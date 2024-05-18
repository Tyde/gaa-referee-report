import {z} from "zod";

export const Rule = z.object({
    id: z.number(),
    code: z.number(),
    isCaution: z.boolean(),
    isBlack: z.boolean(),
    isRed: z.boolean(),
    description: z.string(),
    isDisabled: z.boolean(),
    descriptionFr: z.string().optional(),
    descriptionDe: z.string().optional(),
    descriptionEs: z.string().optional(),
})
export type Rule = z.infer<typeof Rule>
export const NewRuleDEO = Rule.omit({id: true})
export type NewRuleDEO = z.infer<typeof NewRuleDEO>


export const RuleTranslation = z.object({
    ruleEn: z.string(),
    ruleFr: z.string(),
    ruleDe: z.string(),
    ruleEs: z.string(),
})

export type RuleTranslation = z.infer<typeof RuleTranslation>
