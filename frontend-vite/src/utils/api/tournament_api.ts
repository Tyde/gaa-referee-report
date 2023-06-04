import type {DateTime} from "luxon";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import {DatabaseTournament, PublicTournamentReportDEO, RegionDEO, Tournament} from "@/types/tournament_types";
import {promise} from "zod";


export async function loadTournamentsOnDate(date:DateTime):Promise<Array<DatabaseTournament>> {
    let dateString = date.toISODate()
    return fetch("/api/tournament/find_by_date/"+dateString)
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, DatabaseTournament.array()))

}

export async function loadAllTournaments():Promise<Array<DatabaseTournament>> {
    return fetch("/api/tournament/all")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, DatabaseTournament.array()))
}

export async function uploadNewTournament(tournament:Tournament):Promise<DatabaseTournament> {
    if(tournament.region && tournament.name && tournament.location && tournament.date) {
        let dataWithCorrectDate = {
            name:tournament.name,
            location: tournament.location,
            date:tournament.date.toISODate(),
            region:tournament.region
        }
        return makePostRequest("/api/tournament/new", dataWithCorrectDate)
            .then(data => parseAndHandleDEO(data, DatabaseTournament))

    } else {
        return Promise.reject("Missing required fields")
    }
}

export async function loadAllRegions():Promise<Array<RegionDEO>> {
    return fetch("/api/region/all")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, RegionDEO.array()))
}

export async function loadPublicTournamentReport(id:number):Promise<PublicTournamentReportDEO> {
    return fetch("/api/tournament/complete_report/"+id)
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, PublicTournamentReportDEO))
}