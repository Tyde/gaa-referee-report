import {z} from "zod";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import type {Team} from "@/types/team_types";
import {InjuryDEO} from "@/types/game_report_types";
import type {Injury} from "@/types/game_report_types";
function injuryToInjuryDEO(injury: Injury, gameReportId: number) {
    return {
        id: injury.id,
        team: injury.team?.id,
        firstName: injury.firstName,
        lastName: injury.lastName,
        details: injury.details,
        game: gameReportId
    } as InjuryDEO
}
export function injuryDEOToInjury(injuryDEO:InjuryDEO, team:Team):Injury {
    return {
        id: injuryDEO.id,
        firstName: injuryDEO.firstName,
        lastName: injuryDEO.lastName,
        details: injuryDEO.details || "",
        team: team
    } as Injury
}



function checkInjuryReadyForUpload(injury: Injury) {
    return injury.team != undefined &&
        (injury.firstName != "" ||
            injury.lastName != "");
}

export async function uploadInjury(injury: Injury, gameReportId: number):Promise<number> {
    if(checkInjuryReadyForUpload(injury)) {
        let nexturl: string
        if(injury.id != undefined) {
            nexturl = `/api/gamereport/injury/update`
        } else  {
            nexturl = `/api/gamereport/injury/new`
        }
        return makePostRequest(
            nexturl,
            injuryToInjuryDEO(injury, gameReportId)
        )
            .then((data) => parseAndHandleDEO(data, InjuryDEO))
            .then((injuryDEO) => injuryDEO.id)
    } else {
        return -1
    }
}

export async function deleteInjuryOnServer(injury: Injury):Promise<boolean> {
    if(injury.id != undefined) {
        return makePostRequest(
            `/api/gamereport/injury/delete`,
            {id: injury.id}
        )
            .then((data) => parseAndHandleDEO(data, z.object({id: z.number()})))
            .then((data) => data.id == injury.id)
    }
    return Promise.reject("Trying to delete an injury on the server that has not been saved to the server")
}

export function injuryIsBlank(injury: Injury) {
    return injury.firstName.trim().length == 0 && injury.lastName.trim().length == 0 && injury.details.trim().length == 0
}