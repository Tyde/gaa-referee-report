import {string, z} from "zod";
import type {Report} from "@/types/report_types";

export const PitchDEO = z.object({
    id: z.number().nullable().optional(),
    report: z.number().nullable().optional(),
    name: string(),
    surface: z.number().nullable().optional(),
    length: z.number().nullable().optional(),
    width: z.number().nullable().optional(),
    smallSquareMarkings: z.number().nullable().optional(),
    penaltySquareMarkings: z.number().nullable().optional(),
    thirteenMeterMarkings: z.number().nullable().optional(),
    twentyMeterMarkings: z.number().nullable().optional(),
    longMeterMarkings: z.number().nullable().optional(),
    goalPosts: z.number().nullable().optional(),
    goalDimensions: z.number().nullable().optional(),
    additionalInformation: z.string()
})
export type PitchDEO = z.infer<typeof PitchDEO>;

export enum PitchPropertyType {
    surface,
    length,
    width,
    markingsOptions,
    goalPosts,
    goalDimensions,
}

const PitchPropertyTypeEnum = z.nativeEnum(PitchPropertyType)
export const PitchPropertyWithTypeDEO = z.object({
    id: z.number().nullable().optional(),
    name: z.string(),
    type: PitchPropertyTypeEnum
})
export type PitchPropertyWithTypeDEO = z.infer<typeof PitchPropertyWithTypeDEO>;

export interface PitchProperty {
    id: number,
    name: string,
    type: PitchPropertyType,

    disabled?: boolean,
}

export interface Pitch {
    id?: number,
    report: Report,
    name: string,
    surface?: PitchProperty,
    length?: PitchProperty,
    width?: PitchProperty,
    smallSquareMarkings?: PitchProperty,
    penaltySquareMarkings?: PitchProperty,
    thirteenMeterMarkings?: PitchProperty,
    twentyMeterMarkings?: PitchProperty,
    longMeterMarkings?: PitchProperty,
    goalPosts?: PitchProperty,
    goalDimensions?: PitchProperty,
    additionalInformation: string,
}

const PitchProperyDEO = z.object({
    id: z.number(),
    name: z.string().min(1),
    disabled: z.boolean().optional().default(false)
})
type PitchProperyDEO = z.infer<typeof PitchProperyDEO>
export const PitchVariablesDEO = z.object({
    surfaces: PitchProperyDEO.array(),
    lengths: PitchProperyDEO.array(),
    widths: PitchProperyDEO.array(),
    markingsOptions: PitchProperyDEO.array(),
    goalPosts: PitchProperyDEO.array(),
    goalDimensions: PitchProperyDEO.array()
})
export type PitchVariablesDEO = z.infer<typeof PitchVariablesDEO>;

export interface PitchVariables {
    surfaces: Array<PitchProperty>,
    lengths: Array<PitchProperty>,
    widths: Array<PitchProperty>,
    markingsOptions: Array<PitchProperty>,
    goalPosts: Array<PitchProperty>,
    goalDimensions: Array<PitchProperty>,
}