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
    ruleNumber: z.string().nullable().optional(),
    ruleNumberSortKey: z.string().nullable().optional(),
    superseeds: z.number().nullable().optional(),
    isLatest: z.boolean().optional(),
    createdAt: z.string().nullable().optional(),
    createdBy: z.number().nullable().optional(),
    lineageRootId: z.number().nullable().optional(),
})
export type Rule = z.infer<typeof Rule>

export const NewRuleDEO = z.object({
    code: z.number(),
    isCaution: z.boolean(),
    isBlack: z.boolean(),
    isRed: z.boolean(),
    description: z.string(),
    isDisabled: z.boolean(),
    ruleNumber: z.string().nullable().optional(),
    descriptionFr: z.string().optional(),
    descriptionDe: z.string().optional(),
    descriptionEs: z.string().optional(),
})
export type NewRuleDEO = z.infer<typeof NewRuleDEO>

export const NewRuleVersionDEO = z.object({
    parentId: z.number(),
    code: z.number(),
    isCaution: z.boolean(),
    isBlack: z.boolean(),
    isRed: z.boolean(),
    description: z.string(),
    isDisabled: z.boolean(),
    ruleNumber: z.string().nullable().optional(),
    descriptionFr: z.string().optional(),
    descriptionDe: z.string().optional(),
    descriptionEs: z.string().optional(),
})
export type NewRuleVersionDEO = z.infer<typeof NewRuleVersionDEO>

export const RuleHistoryDEO = z.object({
    lineageRootId: z.number(),
    versions: z.array(Rule),
})
export type RuleHistoryDEO = z.infer<typeof RuleHistoryDEO>


export const RuleTranslation = z.object({
    ruleEn: z.string(),
    ruleFr: z.string(),
    ruleDe: z.string(),
    ruleEs: z.string(),
})

export type RuleTranslation = z.infer<typeof RuleTranslation>