import type {Team} from "@/types/team_types";
import {normalizeForSearch} from "@/utils/team_name_normalizer";

export interface TeamSearchResult {
    team: Team,
    score: number,
    /** Set only when the team was found via an alias and not its canonical name. */
    matchedAlias?: string
}

function compareStrings(left: string, right: string): number {
    if (left < right) {
        return -1;
    }
    if (left > right) {
        return 1;
    }
    return 0;
}

function compareTeamNames(left: Team, right: Team): number {
    return compareStrings(normalizeForSearch(left.name), normalizeForSearch(right.name))
        || compareStrings(left.name, right.name)
        || left.id - right.id;
}

const SCORE_CANONICAL_EXACT = 100;
const SCORE_CANONICAL_PREFIX = 80;
const SCORE_CANONICAL_SUBSTRING = 60;
const SCORE_ALIAS_EXACT = 50;
const SCORE_ALIAS_PREFIX = 40;
const SCORE_ALIAS_SUBSTRING = 30;
const SCORE_MEMBER = 20;

function scoreText(
    candidate: string,
    normalizedTerm: string,
    exactScore: number,
    prefixScore: number,
    substringScore: number
): number {
    const normalizedCandidate = normalizeForSearch(candidate);
    if (normalizedCandidate === normalizedTerm) {
        return exactScore;
    }
    if (normalizedCandidate.startsWith(normalizedTerm)) {
        return prefixScore;
    }
    if (normalizedCandidate.includes(normalizedTerm)) {
        return substringScore;
    }
    return 0;
}

/**
 * Ranks teams against a search term. Canonical names always outrank aliases,
 * so familiar results stay on top and aliases fill in otherwise missing
 * results.
 */
export function searchTeams(teams: Team[], searchTerm: string): TeamSearchResult[] {
    const normalizedTerm = normalizeForSearch(searchTerm);

    if (!normalizedTerm) {
        return [...teams]
            .sort(compareTeamNames)
            .map(team => ({team, score: 0}));
    }

    const results: TeamSearchResult[] = [];
    for (const team of teams) {
        const canonicalScore = scoreText(
            team.name,
            normalizedTerm,
            SCORE_CANONICAL_EXACT,
            SCORE_CANONICAL_PREFIX,
            SCORE_CANONICAL_SUBSTRING
        );

        let aliasScore = 0;
        let matchedAlias: string | undefined;
        for (const alias of team.aliases ?? []) {
            const score = scoreText(
                alias.alias,
                normalizedTerm,
                SCORE_ALIAS_EXACT,
                SCORE_ALIAS_PREFIX,
                SCORE_ALIAS_SUBSTRING
            );
            if (score > aliasScore || (
                score > 0
                && score === aliasScore
                && (matchedAlias === undefined || alias.alias < matchedAlias)
            )) {
                aliasScore = score;
                matchedAlias = alias.alias;
            }
        }

        const memberScore = (team.amalgamationTeams ?? []).some(member =>
            normalizeForSearch(member.name).includes(normalizedTerm)
        ) ? SCORE_MEMBER : 0;

        const score = Math.max(canonicalScore, aliasScore, memberScore);
        if (score > 0) {
            results.push({
                team,
                score,
                matchedAlias: canonicalScore > 0 ? undefined : matchedAlias
            });
        }
    }

    return results.sort((a, b) => b.score - a.score || compareTeamNames(a.team, b.team));
}
