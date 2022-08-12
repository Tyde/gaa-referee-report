import type {DateTime} from "luxon";
import type {DatabaseTournament} from "@/types";


export async function loadTournamentsOnDate(date:DateTime):Promise<Array<DatabaseTournament>> {
    let dateString = date.toISODate()
    const res = await(fetch("/api/tournament/find_by_date/"+dateString))
    let json_res = await res.json()
    //TODO properly check correct resposnse
    let tempResponse:Array<{id:number,name:string,location:string,date:string }> = json_res
    return tempResponse.map(value => {
        return {
            id:value.id,
            name:value.name,
            date:new Date(value.date),
            location:value.location
        } as DatabaseTournament
    })
}
