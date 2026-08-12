import type {TeamGameDEO} from "@/types/team_types";

export type PlayedAsOutcome = "W" | "D" | "L";

export interface TeamGameTournamentGroup {
    tournament: TeamGameDEO["tournament"],
    games: TeamGameDEO[]
}

export function gaaScoreTotal(goals: number, points: number): number {
    return goals * 3 + points;
}

export function formatGaaScore(goals: number, points: number): string {
    return `${goals}-${points} (${gaaScoreTotal(goals, points)})`;
}

export function playedAsOutcome(game: TeamGameDEO): PlayedAsOutcome {
    const playedAsTotal = gaaScoreTotal(game.playedAsGoals, game.playedAsPoints);
    const opponentTotal = gaaScoreTotal(game.opponentGoals, game.opponentPoints);
    if (playedAsTotal > opponentTotal) {
        return "W";
    }
    if (playedAsTotal < opponentTotal) {
        return "L";
    }
    return "D";
}

export function sortTeamGamesByStartTime(games: TeamGameDEO[]): TeamGameDEO[] {
    return games.toSorted((first, second) => {
        if (first.startTime === null) {
            return second.startTime === null ? 0 : 1;
        }
        if (second.startTime === null) {
            return -1;
        }
        return first.startTime.toMillis() - second.startTime.toMillis();
    });
}

export function groupAndSortTeamGames(games: TeamGameDEO[]): TeamGameTournamentGroup[] {
    const groupsByTournamentId = new Map<number, TeamGameTournamentGroup>();
    for (const game of games) {
        const group = groupsByTournamentId.get(game.tournament.id);
        if (group) {
            group.games.push(game);
        } else {
            groupsByTournamentId.set(game.tournament.id, {
                tournament: game.tournament,
                games: [game]
            });
        }
    }
    return Array.from(groupsByTournamentId.values())
        .map(group => ({...group, games: sortTeamGamesByStartTime(group.games)}))
        .toSorted((first, second) => tournamentRelevantDate(second.tournament).toMillis() - tournamentRelevantDate(first.tournament).toMillis());
}

function tournamentRelevantDate(tournament: TeamGameDEO["tournament"]) {
    return tournament.isLeague && tournament.endDate ? tournament.endDate : tournament.date;
}
