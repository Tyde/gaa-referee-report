import {describe, expect, it} from "vitest";
import {normalizeForSearch} from "@/utils/team_name_normalizer";

/**
 * Shared normalization contract. The Kotlin implementation in
 * src/main/kotlin/eu/gaelicgames/referee/util/TeamNameNormalizer.kt is tested
 * against the exact same vectors. Keep both lists in sync.
 */
const vectors: Array<[string, string]> = [
    ["Zürich", "zurich"],
    ["Zurich", "zurich"],
    ["ZÜRICH", "zurich"],
    ["Saint-Brieuc", "saint-brieuc"],
    ["Gaélique Bro Sant Brieg", "gaelique bro sant brieg"],
    ["  Padded  Name  ", "padded name"],
    ["Straßburg", "strassburg"],
    ["Ærø", "aero"],
    ["Œuvre", "oeuvre"],
    ["Þórshöfn", "thorshofn"],
    ["Łódź", "lodz"],
    ["Ðjurgården", "djurgarden"],
    ["A\u00a0B", "a\u00a0b"],
    ["", ""]
];

describe("normalizeForSearch", () => {
    it("normalizes shared vectors", () => {
        for (const [input, expected] of vectors) {
            expect(normalizeForSearch(input), `input was: '${input}'`).toBe(expected);
        }
    });

    it("is idempotent", () => {
        for (const [input] of vectors) {
            const once = normalizeForSearch(input);
            expect(normalizeForSearch(once)).toBe(once);
        }
    });

    it("does not fold punctuation", () => {
        expect(normalizeForSearch("Saint-Brieuc")).toBe("saint-brieuc");
        expect(normalizeForSearch("Saint Brieuc")).toBe("saint brieuc");
    });
});
