import type {DateTime} from "luxon";
import {parseAndHandleDEO} from "@/utils/api/api_utils";
import {DatabaseTournament} from "@/types/tournament_types";


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