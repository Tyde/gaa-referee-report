import {z} from "zod";

export const GameCode = z.object({
    id: z.number(),
    name: z.string().min(1),
})

export type GameCode = z.infer<typeof GameCode>



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

export const GameLengthOption = z.object({
    id: z.number(),
    name: z.string().min(1),
    minutes: z.number(),
})
export type GameLengthOption = z.infer<typeof GameLengthOption>


export const ApiErrorOptions = z.enum([
    "insertionFailed", "notFound", "deleteFailed", "notAuthorized"
])
export type ApiErrorOptions = z.infer<typeof ApiErrorOptions>;

export const ApiError = z.object({
    error: ApiErrorOptions,
    message: z.string().nullable().optional()
})
export type ApiError = z.infer<typeof ApiError>;

export function isApiError(obj: any): obj is ApiError {
    return obj.error !== undefined;
}

export const DeletionStateEnum = z.enum(["DELETED", "DISABLED", "FAILED"])

export const DeletionResponse = z.object({
    id: z.number(),
    deletionState: DeletionStateEnum
})

export type DeletionResponse = z.infer<typeof DeletionResponse>

export class ErrorMessage {
    message: string;
    timestamp: number;

    constructor(message: string) {
        this.message = message;
        this.timestamp = Date.now();
    }
}
