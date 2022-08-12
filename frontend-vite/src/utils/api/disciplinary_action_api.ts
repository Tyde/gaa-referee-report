import type {DisciplinaryAction, DisciplinaryActionDEO, Rule, Team} from "@/types";

export function fromDisciplinaryActionDEOToDisciplinaryAction(
    dADEO:DisciplinaryActionDEO,
    allRules:Rule[],
    team:Team
):DisciplinaryAction {
    return {
        id: dADEO.id,
        team: team,
        firstName: dADEO.firstName,
        lastName: dADEO.lastName,
        number: dADEO.number,
        rule: allRules.find(rule => rule.id == dADEO.rule),
        details: dADEO.details,
    } as DisciplinaryAction
}

export function  checkActionReadyForUpload(dAction: DisciplinaryAction) {
    return dAction.rule != undefined &&
        (dAction.firstName != "" ||
            dAction.lastName != "") &&
        dAction.details != "" &&
        dAction.number != undefined
}

export async function uploadDisciplinaryAction(action: DisciplinaryAction, gameReportId:number):Promise<number> {
    if (checkActionReadyForUpload(action)) {
        const requestOptions = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json;charset=utf-8'
            },
            body: JSON.stringify({
                "id": action.id,
                "team": action.team?.id,
                "firstName": action.firstName,
                "lastName": action.lastName,
                "number": action.number,
                "details": action.details,
                "rule": action.rule?.id,
                "game": gameReportId
            })
        }
        let nexturl = ""
        if (action.id != undefined) {
            nexturl = `/api/gamereport/disciplinaryAction/update`
        } else {
            nexturl = `/api/gamereport/disciplinaryAction/new`
        }
        const response = await fetch(nexturl, requestOptions)
        const data = await response.json()
        if (data.id) {
            console.log("Update complete")
            if (action.id == undefined) {
                action.id = data.id
            }
            return data.id
        } else {
            console.log(data)
            return -1
        }
    } else {
        console.log("Action not ready for upload")
        console.log(action)
        return -2
    }

}