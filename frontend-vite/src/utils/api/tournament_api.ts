import type {DateTime} from "luxon";
import {ApiError, DatabaseTournament} from "@/types";
import {z} from "zod";


export async function loadTournamentsOnDate(date:DateTime):Promise<Array<DatabaseTournament>> {
    let dateString = date.toISODate()
    const res = await(fetch("/api/tournament/find_by_date/"+dateString))
    let json_res = await res.json()
    //TODO properly check correct resposnse
    let parseResonse = DatabaseTournament.array().safeParse(json_res)
    if(parseResonse.success) {
        return parseResonse.data
    } else {
        let errorParse = ApiError.safeParse(json_res)
        if(errorParse.success) {
            return Promise.reject(errorParse.data.message)
        } else {
            return Promise.reject(parseResonse.error)
        }
    }
}
