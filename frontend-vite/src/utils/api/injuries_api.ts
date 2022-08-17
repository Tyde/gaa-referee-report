import type {Injury, InjuryDEO, Team} from "@/types";

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
        details: injuryDEO.details,
        team: team
    } as Injury
}



function checkInjuryReadyForUpload(injury: Injury) {
    return injury.team != undefined &&
        (injury.firstName != "" ||
            injury.lastName != "") &&
        injury.details != "";
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
        let nexturl = ""
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