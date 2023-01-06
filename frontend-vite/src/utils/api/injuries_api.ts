import type {Injury, InjuryDEO, Team} from "@/types";
import {ApiError} from "@/types";
import {z} from "zod";

function injuryToInjuryDAO(injury: Injury, gameReportId: number) {
    return {
        id: injury.id,
        team: injury.team?.id,
        firstName: injury.firstName,
        lastName: injury.lastName,
        details: injury.details,
        game: gameReportId
    };
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
        const requestOptions = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json;charset=utf-8'
            },
            body: JSON.stringify(injuryToInjuryDAO(injury, gameReportId))
        }
        let nexturl: string
        if(injury.id != undefined) {
            nexturl = `/api/gamereport/injury/update`
        } else  {
            nexturl = `/api/gamereport/injury/new`
        }
        const response = await fetch(nexturl, requestOptions)
        const data = await response.json()
        if (data.id) {
            console.log("Update complete")
            if(injury.id === undefined) {
                injury.id = data.id
            }
            return data.id
        } else {
            console.log(data)
            return -1
        }
    }
    return -1;
}

export async function deleteInjuryOnServer(injury: Injury):Promise<boolean> {
    if(injury.id != undefined) {
        const requestOptions = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json;charset=utf-8'
            },
            body: JSON.stringify({id: injury.id})
        }
        const response = await fetch(`/api/gamereport/injury/delete`, requestOptions)
        const data = await response.json()
        const parseResult = z.object({
            id: z.number()
        }).safeParse(data)
        if(parseResult.success) {
            return true
        } else {
            const epR = ApiError.safeParse(data)
            if (epR.success) {
                return Promise.reject(epR.data.message)
            } else {
                return Promise.reject("Unknown error")
            }
        }
    }
    return Promise.reject("Trying to delete an injury on the server that has not been saved to the server")
}

export function injuryIsBlank(injury: Injury) {
    return injury.firstName.trim().length == 0 && injury.lastName.trim().length == 0 && injury.details.trim().length == 0
}