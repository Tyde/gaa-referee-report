import {z} from "zod";
import type {ExtraTimeOption} from "@/types";
import {GameCode, GameType} from "@/types";
import {gameReportDEOToGameReport} from "@/utils/api/game_report_api";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import type {DatabaseTournament} from "@/types/tournament_types";
import type {Report, ReportDEO} from "@/types/report_types";
import {
    CompactTournamentReportDEO,
    CompleteReportDEO,
    NewReportDEO,
    UpdateReportAdditionalInformationDEO
} from "@/types/report_types";
import type {GameReport, CompleteGameReportDEO} from "@/types/game_report_types";
import type {Rule} from "@/types/rules_types";


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
    return fetch("/api/report/get/" + id)
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data,CompleteReportDEO))
}

export async function uploadReport(
    report:Report
):Promise<number> {
    let address: string
    if(report.id != undefined) {
        address = "/api/report/update"
    } else {
        address = "/api/report/new"
    }
    return makePostRequest(
        address,
        NewReportDEO.parse({
            id: report.id,
            tournament: report.tournament.id,
            selectedTeams: report.selectedTeams.map(value => value.id),
            gameCode: report.gameCode.id
        })
    )
        .then(data => parseAndHandleDEO(data, NewReportDEO))
        .then(data => {
            if(data.id != undefined) {
                return data.id
            } else {
                return Promise.reject("Could not get id from server")
            }
        })
}

export async function updateReportAdditionalInformation(report:Report):Promise<number> {
    return makePostRequest(
        "/api/report/updateAdditionalInformation",
        UpdateReportAdditionalInformationDEO.parse({
            id: report.id,
            additionalInformation: report.additionalInformation
        })
    )
        .then(data => parseAndHandleDEO(data, UpdateReportAdditionalInformationDEO))
        .then(data => {
            if(data.id != undefined) {
                return data.id
            } else {
                return Promise.reject("Could not get id from server")
            }
        })
}


const SubmitReportDEO = z.object({
    id: z.number(),
})

export async function submitReportToServer(report:Report):Promise<number> {
    let submitReportObj = SubmitReportDEO.parse({
        id: report.id
    })
    return makePostRequest(
        "/api/report/submit",
        submitReportObj
    )
        .then(data => parseAndHandleDEO(data, SubmitReportDEO))
        .then(data => {
            if(data.id != undefined) {
                return data.id
            } else {
                return Promise.reject("Could not get id from server")
            }
        })
}

export function ReportDEOToReport(reportDEO:ReportDEO,tournaments:Array<DatabaseTournament>):Report {
    return {
        id: reportDEO.id,
        tournament: tournaments.find(tournament => tournament.id === reportDEO.tournament),

    } as Report
}
export async function loadAllReports(tournaments:Array<DatabaseTournament>):Promise<Array<CompactTournamentReportDEO>> {
    return fetch("/api/report/all")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, CompactTournamentReportDEO.array()))
}

export async function loadMyReports():Promise<Array<CompactTournamentReportDEO>> {
    return fetch("/api/report/my")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, CompactTournamentReportDEO.array()))
}

export async function deleteReportOnServer(reportId:number):Promise<number> {
    return makePostRequest("/api/report/delete",{id: reportId})
        .then(data => parseAndHandleDEO(data, z.object({id: z.number()})))
        .then(data =>  data.id)
}