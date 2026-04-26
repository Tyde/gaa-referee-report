import { z } from "zod";

export const TeamTournamentCountDEO = z.object({
    teamId: z.number(),
    teamName: z.string(),
    tournamentCount: z.number()
});
export type TeamTournamentCountDEO = z.infer<typeof TeamTournamentCountDEO>;

export const CardStatsByYearDEO = z.object({
    year: z.number(),
    cautionCount: z.number(),
    blackCardCount: z.number(),
    redCardCount: z.number(),
    totalGames: z.number()
});
export type CardStatsByYearDEO = z.infer<typeof CardStatsByYearDEO>;

export const CardStatsByRegionDEO = z.object({
    regionId: z.number(),
    regionName: z.string(),
    cautionCount: z.number(),
    blackCardCount: z.number(),
    redCardCount: z.number(),
    totalGames: z.number()
});
export type CardStatsByRegionDEO = z.infer<typeof CardStatsByRegionDEO>;

export const AverageCardsPerGameDEO = z.object({
    averageCautions: z.number(),
    averageBlackCards: z.number(),
    averageRedCards: z.number(),
    totalGames: z.number()
});
export type AverageCardsPerGameDEO = z.infer<typeof AverageCardsPerGameDEO>;

export const TeamEloDEO = z.object({
    teamId: z.number(),
    teamName: z.string(),
    eloScore: z.number(),
    gamesPlayed: z.number()
});
export type TeamEloDEO = z.infer<typeof TeamEloDEO>;

export const StatsDEO = z.object({
    teamTournamentCounts: z.array(TeamTournamentCountDEO),
    cardsByYear: z.array(CardStatsByYearDEO),
    cardsByRegion: z.array(CardStatsByRegionDEO),
    averageCardsPerGame: AverageCardsPerGameDEO,
    teamElos: z.array(TeamEloDEO)
});
export type StatsDEO = z.infer<typeof StatsDEO>;
