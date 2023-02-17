import {DateTime} from "luxon";
import {number, string, z} from "zod";

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

export const Tournament = z.object({
    name: z.string().min(1),
    location: z.string().min(1),
    date: z.string().transform((value) => DateTime.fromISO(value))
})
export type Tournament = z.infer<typeof Tournament>
export const DatabaseTournament = Tournament.extend({
    id: z.number()
})
export type DatabaseTournament = z.infer<typeof DatabaseTournament>

export const GameCode = z.object({
    id: z.number(),
    name: z.string().min(1),
})

export type GameCode = z.infer<typeof GameCode>
export type Report = z.infer<typeof Report>

export const Referee = z.object({
    id: z.number(),
    firstName: z.string(),
    lastName: z.string(),
    mail: z.string(),
})


export type Referee = z.infer<typeof Referee>


export const SessionInfo = Referee.extend({
    role: z.enum(["ADMIN", "REFEREE", "INACTIVE", "WAITING_FOR_ACTIVATION"])
})
export type SessionInfo = z.infer<typeof SessionInfo>

export const Report = z.object({
    id: number().optional().nullable(),
    tournament: DatabaseTournament,
    selectedTeams: Team.array(),
    gameCode: GameCode,
    additionalInformation: z.string().optional(),
    isSubmitted: z.boolean().optional().nullable(),
    submitDate: z.string().optional().nullable().transform((value) => {
        if (value) {
            return DateTime.fromISO(value)
        } else {
            return null
        }
    }),
    referee: Referee
})


export const ReportDEO = z.object({
        id: number(),
        tournament: number(),
        code: number(),
        additionalInformation: z.string().optional(),
        isSubmitted: z.boolean(),
        submitDate: z.string().optional().nullable().transform((value) => {
            if (value) {
                return DateTime.fromISO(value)
            } else {
                return null
            }
        }),
    }
)
export type ReportDEO = z.infer<typeof ReportDEO>



export const GameType = z.object({
    id: z.number(),
    name: z.string().min(1),
})
export type GameType = z.infer<typeof GameType>

export const ExtraTimeOption = z.object({
    id: z.number(),
    name: z.string().min(1),
})
export type ExtraTimeOption = z.infer<typeof ExtraTimeOption>


export interface GameReport {
    id?: number
    report: Report,
    startTime?: DateTime,
    gameType?: GameType,
    teamAReport: SingleTeamGameReport,
    teamBReport: SingleTeamGameReport,
    extraTime?: ExtraTimeOption,
    umpirePresentOnTime: boolean,
    umpireNotes: string
}

export const InjuryDEO = z.object({
    id: z.number(),
    firstName: z.string(),
    lastName: z.string(),
    details: z.string(),
    team: z.number()
})
export type InjuryDEO = z.infer<typeof InjuryDEO>

export interface Injury {
    id?: number,
    firstName: string,
    lastName: string,
    details: string,
    team?: Team,
}

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


export const DisciplinaryActionDEO = z.object({
    id: z.number(),
    team: z.number(),
    firstName: z.string(),
    lastName: z.string(),
    number: z.number(),
    rule: z.number(),
    details: z.string(),
})
export type DisciplinaryActionDEO = z.infer<typeof DisciplinaryActionDEO>

export interface DisciplinaryAction {
    id?: number,
    team?: Team,
    firstName: string,
    lastName: string,
    number?: number,
    rule?: Rule,
    details: string,
}

export interface SingleTeamGameReport {
    team?: Team,
    goals?: number,
    points?: number,
    injuries: Injury[],
    disciplinaryActions: DisciplinaryAction[],
}


export const ApiErrorOptions = z.enum(["insertionFailed", "notFound"])
export type ApiErrorOptions = z.infer<typeof ApiErrorOptions>;

export const ApiError = z.object({
    error: ApiErrorOptions,
    message: z.string().nullable().optional()
})
export type ApiError = z.infer<typeof ApiError>;


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

export class ErrorMessage {
    message: string;
    timestamp: number;

    constructor(message: string) {
        this.message = message;
        this.timestamp = Date.now();
    }
}

export interface NewReferee {
    firstName?: string,
    lastName?: string,
    mail?: string,
}