import {describe, expect, it} from "vitest";
import type {Team} from "@/types/team_types";
import {searchTeams} from "@/utils/team_search";

function team(id: number, name: string, aliases: string[] = []): Team {
    return {
        id,
        name,
        isAmalgamation: false,
        amalgamationTeams: null,
        aliases: aliases.map((alias, index) => ({id: id * 100 + index, teamId: id, alias}))
    };
}

const zurich = team(1, "Zürich Inneractive", ["Zurich GAA"]);
const brieuc = team(2, "Gaélique Bro Sant Brieg", ["Saint-Brieuc"]);
const zug = team(3, "Zug Wanderers");
const amalgamation: Team = {
    id: 4,
    name: "Swiss Selection",
    isAmalgamation: true,
    amalgamationTeams: [team(1, "Zürich Inneractive"), team(3, "Zug Wanderers")],
    aliases: []
};
const allTeams = [zurich, brieuc, zug, amalgamation];

describe("searchTeams", () => {
    it("returns every team sorted by name when the term is empty", () => {
        const results = searchTeams(allTeams, "");
        expect(results.map(r => r.team.name)).toEqual([
            "Gaélique Bro Sant Brieg",
            "Swiss Selection",
            "Zug Wanderers",
            "Zürich Inneractive"
        ]);
        expect(results.every(result => result.score === 0)).toBe(true);
    });

    it("finds a team by an alias and reports which alias matched", () => {
        const results = searchTeams(allTeams, "Saint-Brieuc");
        expect(results).toHaveLength(1);
        expect(results[0].team.id).toBe(2);
        expect(results[0].score).toBe(50);
        expect(results[0].matchedAlias).toBe("Saint-Brieuc");
    });

    it("does not report a matched alias when the canonical name also matched", () => {
        const results = searchTeams(allTeams, "Zürich");
        expect(results[0].team.id).toBe(1);
        expect(results[0].score).toBe(80);
        expect(results[0].matchedAlias).toBeUndefined();
    });

    it("ranks canonical matches above alias matches", () => {
        const aliasOnly = team(5, "Completely Different", ["Zug"]);
        const results = searchTeams([aliasOnly, zug], "Zug");
        expect(results.map(r => r.team.id)).toEqual([3, 5]);
        expect(results.map(r => r.score)).toEqual([80, 50]);
    });

    it("ranks exact above prefix above substring", () => {
        const exact = team(6, "Cork");
        const prefix = team(7, "Cork Harlequins");
        const substring = team(8, "New Cork");
        const results = searchTeams([substring, prefix, exact], "Cork");
        expect(results.map(r => r.team.id)).toEqual([6, 7, 8]);
        expect(results.map(r => r.score)).toEqual([100, 80, 60]);
    });

    it("scores alias prefix and substring matches", () => {
        const prefix = team(9, "Canonical Prefix", ["Cork Harlequins"]);
        const substring = team(10, "Canonical Substring", ["New Cork"]);
        const results = searchTeams([substring, prefix], "Cork");
        expect(results.map(r => r.team.id)).toEqual([9, 10]);
        expect(results.map(r => r.score)).toEqual([40, 30]);
    });

    it("chooses the lexically first alias when scores tie", () => {
        const result = searchTeams([team(12, "Canonical", ["Zulu Club", "Zebra Club"])], "Z")[0];
        expect(result.matchedAlias).toBe("Zebra Club");
    });

    it("matches amalgamation member names", () => {
        const results = searchTeams([amalgamation], "Wanderers");
        expect(results).toHaveLength(1);
        expect(results[0].team.id).toBe(4);
        expect(results[0].score).toBe(20);
    });

    it("does not report an alias when a canonical match outranks it", () => {
        const mixed = team(11, "Cork", ["Cork Harlequins"]);
        const results = searchTeams([mixed], "Cork");
        expect(results[0].score).toBe(100);
        expect(results[0].matchedAlias).toBeUndefined();
    });

    it("ignores diacritic differences in the search term", () => {
        expect(searchTeams(allTeams, "zurich").map(r => r.team.id)).toContain(1);
    });

    it("returns nothing when no team matches", () => {
        expect(searchTeams(allTeams, "Ballinasloe")).toHaveLength(0);
    });
});
