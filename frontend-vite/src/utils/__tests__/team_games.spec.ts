import {describe, expect, it} from "vitest";
import {DateTime} from "luxon";
import type {TeamGameDEO} from "@/types/team_types";
import {
    formatGaaScore,
    gaaScoreTotal,
    groupAndSortTeamGames,
    playedAsOutcome,
    sortTeamGamesByStartTime
} from "@/utils/team_games";

function game(overrides: Partial<TeamGameDEO> = {}): TeamGameDEO {
    return {
        gameId: 1,
        reportId: 10,
        tournament: {
            id: 100,
            name: "Spring Tournament",
            location: "Dublin",
            date: DateTime.fromISO("2026-03-01"),
            region: 1,
            isLeague: false,
            endDate: null
        },
        startTime: DateTime.fromISO("2026-03-01T10:00:00"),
        playedAsTeam: {id: 2, name: "Dublin GAA", isAmalgamation: false, amalgamationTeams: null},
        opponentTeam: {id: 3, name: "Cork GAA", isAmalgamation: false, amalgamationTeams: null},
        playedAsGoals: 1,
        playedAsPoints: 5,
        opponentGoals: 0,
        opponentPoints: 8,
        refereeId: 4,
        refereeName: "Ref Eree",
        codeId: 5,
        codeName: "Football",
        gameTypeId: null,
        gameTypeName: null,
        ...overrides
    };
}

describe("team game view helpers", () => {
    it("calculates a GAA score total as goals times three plus points", () => {
        expect(gaaScoreTotal(2, 4)).toBe(10);
    });

    it("formats a GAA score with its total", () => {
        expect(formatGaaScore(2, 4)).toBe("2-4 (10)");
    });

    it("returns the played-as outcome", () => {
        expect(playedAsOutcome(game({playedAsGoals: 2, playedAsPoints: 1, opponentGoals: 1, opponentPoints: 3}))).toBe("W");
        expect(playedAsOutcome(game({playedAsGoals: 1, playedAsPoints: 4, opponentGoals: 2, opponentPoints: 1}))).toBe("D");
        expect(playedAsOutcome(game({playedAsGoals: 0, playedAsPoints: 8, opponentGoals: 1, opponentPoints: 6}))).toBe("L");
    });

    it("groups games by tournament and orders tournaments newest first", () => {
        const league = game({
            gameId: 1,
            tournament: {
                id: 11,
                name: "League",
                location: "Galway",
                date: DateTime.fromISO("2026-01-10"),
                region: 1,
                isLeague: true,
                endDate: DateTime.fromISO("2026-05-01")
            }
        });
        const tournament = game({
            gameId: 2,
            tournament: {
                id: 12,
                name: "Cup",
                location: "Limerick",
                date: DateTime.fromISO("2026-04-20"),
                region: 1,
                isLeague: false,
                endDate: null
            }
        });
        const anotherLeagueGame = game({gameId: 3, tournament: league.tournament});

        const groups = groupAndSortTeamGames([tournament, league, anotherLeagueGame]);

        expect(groups.map(group => group.tournament.id)).toEqual([11, 12]);
        expect(groups[0].games.map(item => item.gameId)).toEqual([1, 3]);
    });

    it("orders games with start times chronologically and puts missing times last", () => {
        const games = [
            game({gameId: 1, startTime: DateTime.fromISO("2026-03-01T14:00:00")}),
            game({gameId: 2, startTime: null}),
            game({gameId: 3, startTime: DateTime.fromISO("2026-03-01T09:00:00")})
        ];

        expect(sortTeamGamesByStartTime(games).map(item => item.gameId)).toEqual([3, 1, 2]);
    });
});
