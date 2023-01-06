import {number, z} from "zod";
import type {ExtraTimeOption, GameReport, Rule} from "@/types";
import {ApiError, DatabaseTournament, GameCode, GameType, PitchDEO, Referee, Report, Team} from "@/types";
import {DateTime} from "luxon";
import {CompleteGameReportDEO, gameReportDEOToGameReport} from "@/utils/api/game_report_api";


export async function getGameCodes(): Promise<Array<GameCode>> {
    return fetch("/api/codes")
        .then(response => response.json())
        .then(data => z.array(GameCode).safeParse(data))
        .then(parseResult => {
            if (parseResult.success) {
                return parseResult.data
            } else {
                return Promise.reject("Could not load or parse game codes")
            }
        })
}


export const CompleteReportDEO = z.object({
    id: z.number(),
    tournament: DatabaseTournament,
    code: z.number(),
    additionalInformation: z.string().optional(),
    isSubmitted: z.boolean(),
    submitDate: z.string().transform((value) => DateTime.fromISO(value)).nullable(),
    selectedTeams: Team.array(),
    gameReports: CompleteGameReportDEO.array(),
    pitches: PitchDEO.array().nullable(),
    referee: Referee
})
export type CompleteReportDEO = z.infer<typeof CompleteReportDEO>;

export function completeReportDEOToReport(cReport: CompleteReportDEO, availableCodes: Array<GameCode>): Report {

    const code = availableCodes.find(code => code.id === cReport.code)
    if (code != undefined) {
        return {
            tournament: cReport.tournament,
            selectedTeams: cReport.selectedTeams,
            gameCode: code,
            id: cReport.id,
            additionalInformation: cReport.additionalInformation,
            isSubmitted: cReport.isSubmitted,
            submitDate: cReport.submitDate,
            referee: cReport.referee
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
    return cReport.gameReports.map((gameReportDEO: CompleteGameReportDEO) => {
        return gameReportDEOToGameReport(
            gameReportDEO,
            report,
            gameTypes,
            extraTimeOptions,
            rules
        )
    }).filter(gameReport => gameReport != undefined)
}

export async function loadReportDEO(id: number): Promise<CompleteReportDEO> {
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

const NewReportDEO = z.object({
    id: z.number().nullable().optional(),
    tournament: number(),
    gameCode: number(),
    selectedTeams: number().array(),
});

export async function uploadReport(
    report:Report
):Promise<number> {
    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify(NewReportDEO.parse({
            id: report.id,
            tournament: report.tournament.id,
            selectedTeams: report.selectedTeams.map(value => value.id),
            gameCode: report.gameCode.id
        }))
    };
    let address = ""
    if(report.id != undefined) {
        address = "/api/report/update"
    } else {
        address = "/api/report/new"
    }
    const response = await fetch(address, requestOptions)
    const dbReport = await response.json()
    const parseResponse = NewReportDEO.safeParse(dbReport)
    if(parseResponse.success) {
        report.id = parseResponse.data.id
        return (parseResponse.data.id || -1)
    } else {
        return Promise.reject("Server did not return a valid report")
    }
}

export const UpdateReportAdditionalInformationDEO = z.object({
    id: z.number(),
    additionalInformation: z.string()
})

export type UpdateReportAdditionalInformationDEO = z.infer<typeof UpdateReportAdditionalInformationDEO>;
export async function updateReportAdditionalInformation(report:Report):Promise<number> {
    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify(UpdateReportAdditionalInformationDEO.parse({
            id: report.id,
            additionalInformation: report.additionalInformation
        }))
    };
    let address = ""

    address = "/api/report/updateAdditionalInformation"

    const response = await fetch(address, requestOptions)
    const dbReport = await response.json()
    const parseResponse = UpdateReportAdditionalInformationDEO.safeParse(dbReport)
    if(parseResponse.success) {
        report.id = parseResponse.data.id
        return (parseResponse.data.id || -1)
    } else {
        return Promise.reject("Server did not return a valid response")
    }
}


const SubmitReportDEO = z.object({
    id: z.number(),
})

export async function submitReportToServer(report:Report):Promise<number> {
    let submitReportObj = SubmitReportDEO.parse({
        id: report.id
    })
    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify(submitReportObj)
    };
    let address = ""

    address = "/api/report/submit"

    const response = await fetch(address, requestOptions)
    const dbReport = await response.json()
    const parseResponse = SubmitReportDEO.safeParse(dbReport)
    if(parseResponse.success) {
        report.id = parseResponse.data.id
        return (parseResponse.data.id || -1)
    } else {
        return Promise.reject("Server did not return a valid report")
    }
}