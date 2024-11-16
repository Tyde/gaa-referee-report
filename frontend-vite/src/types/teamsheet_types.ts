import {z} from "zod";


export const PlayerDEO = z.object({
    name: z.string(),
    jerseyNumber: z.number().optional(),
    playerNumber: z.number().nullish()
})

export type PlayerDEO = z.infer<typeof PlayerDEO>


export const TeamsheetUploadSuccessDEO = z.object({
    players: PlayerDEO.array(),
    fileKey: z.string()
})

export type TeamsheetUploadSuccessDEO = z.infer<typeof TeamsheetUploadSuccessDEO>
export const TeamsheetWithClubAndTournamentDataDEO = z.object({
        players: PlayerDEO.array(),
        clubId: z.number(),
        tournamentId: z.number(),
        fileKey: z.string(),
        registrarMail: z.string(),
        registrarName: z.string(),
        codeId: z.number()
    })

export type TeamsheetWithClubAndTournamentDataDEO = z.infer<typeof TeamsheetWithClubAndTournamentDataDEO>



export const ReplaceTeamsheetFileDEO = z.object({
    oldfileKey: z.string(),
    newTeamsheetData: TeamsheetWithClubAndTournamentDataDEO
})

export type ReplaceTeamsheetFileDEO = z.infer<typeof ReplaceTeamsheetFileDEO>
