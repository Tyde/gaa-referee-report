import {z} from "zod";
import {
    ApiError,
    DatabaseTournament,
    DisciplinaryActionDEO, ExtraTimeOption,
    GameReport,
    GameType,
    InjuryDEO,
    PitchDEO, Rule,
    Team
} from "@/types";
import type {GameCode, Report} from "@/types";
import {DateTime} from "luxon";
import {CompleteGameReportDEO, GameReportDEO, gameReportDEOToGameReport} from "@/utils/api/game_report_api";



export const CompleteReportDEO = z.object({
    id: z.number(),
    tournament: DatabaseTournament,
    code: z.number(),
    additionalInformation: z.string().nullable(),
    isSubmitted: z.boolean(),
    submitDate: z.string().transform((value) => DateTime.fromISO(value)).nullable(),
    selectedTeams: Team.array(),
    gameReports: CompleteGameReportDEO.array(),
    pitches: PitchDEO.array().nullable()
})
export type CompleteReportDEO = z.infer<typeof CompleteReportDEO>;

export function completeReportDEOToReport(cReport: CompleteReportDEO, availableCodes: Array<GameCode>): Report {
    let code = availableCodes.find(code => code.id === cReport.code)
    if (code != undefined) {
        return {
            tournament: cReport.tournament,
            selectedTeams: cReport.selectedTeams,
            gameCode: code,
            id: cReport.id,
        }
    } else {
        throw new Error("Could not find game code")
    }
}

export function extractGameReportsFromCompleteReportDEO(
    cReport:CompleteReportDEO,
    report:Report,
    gameTypes: Array<GameType>,
    extraTimeOptions: Array<ExtraTimeOption>,
    rules: Array<Rule>
): Array<GameReport> {
    // @ts-ignore
    let out:Array<GameReport> = cReport.gameReports.map((gameReportDEO:CompleteGameReportDEO) => {
        return gameReportDEOToGameReport(
            gameReportDEO,
            report,
            gameTypes,
            extraTimeOptions,
            rules
        )
    }).filter(gameReport => gameReport != undefined)
    return out
}

export async function loadReport(id: number): Promise<CompleteReportDEO> {
    const res = await (fetch("/api/report/get/" + id))
    let json_res = await res.json()

    const parsed = CompleteReportDEO.safeParse(json_res)
    if (parsed.success) {
        return parsed.data
    } else {
        const errorParse = ApiError.safeParse(json_res)
        if (errorParse.success) {
            return Promise.reject(errorParse.data.message)
        }
        return Promise.reject(parsed.error)
    }

}

