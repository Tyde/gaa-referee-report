import {z} from "zod";

export const Referee = z.object({
    id: z.number(),
    firstName: z.string(),
    lastName: z.string(),
    mail: z.string(),
})
export type Referee = z.infer<typeof Referee>



export const UpdateRefereeDAO = Referee.partial({
    firstName: true, lastName: true, mail: true
})
export type UpdateRefereeDAO = z.infer<typeof UpdateRefereeDAO>


export const RefereeRole = z.enum([
    "ADMIN",
    "REFEREE",
    "INACTIVE",
    "WAITING_FOR_ACTIVATION",
    "CCC",
    "CCC_WAITING_FOR_ACTIVATION"
])


export type RefereeRole = z.infer<typeof RefereeRole>
export const RefereeWithRoleDEO = Referee.extend({
    role: RefereeRole
})
export type RefereeWithRoleDEO = z.infer<typeof RefereeWithRoleDEO>
export const UpdateRefereePasswordDAO = z.object({
    id: z.number(),
    oldPassword: z.string(),
    newPassword: z.string()
})
export type UpdateRefereePasswordDAO = z.infer<typeof UpdateRefereePasswordDAO>
export const UpdateRefereePasswordResponse = z.object({
    id: z.number(),
    success: z.boolean(),
    message: z.string().optional().nullable()
})
export type UpdateRefereePasswordResponse = z.infer<typeof UpdateRefereePasswordResponse>

export const SetRefereeRoleDEO = z.object({
    id: z.number(),
    role: RefereeRole
})
export type SetRefereeRoleDEO = z.infer<typeof SetRefereeRoleDEO>

export interface NewReferee {
    firstName?: string,
    lastName?: string,
    mail?: string,
}


