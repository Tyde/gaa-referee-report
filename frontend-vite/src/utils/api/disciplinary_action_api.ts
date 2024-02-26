import z from "zod";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import type {Team} from "@/types/team_types";
import type {DisciplinaryAction} from "@/types/game_report_types";
import {DisciplinaryActionDEO} from "@/types/game_report_types";
import {Rule} from "@/types/rules_types";

export async function getRules():Promise<Rule[]> {
    return fetch("/api/rules")
        .then(response => response.json())
        .then(data => z.array(Rule).safeParse(data))
        .then(parseResult => {
            if (parseResult.success) {
                return parseResult.data
            } else {
                return Promise.reject("Could not load or parse rules")
            }
        })
}
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
        redCardIssued: dADEO.redCardIssued,
        forTeamOfficial: dADEO.number == -1
    } as DisciplinaryAction
}

export function fromDisciplinaryActionToDisciplinaryActionDEO(
    dA:DisciplinaryAction,
    gameReportId:number
):DisciplinaryActionDEO {
    return {
        id: dA.id,
        team: dA.team?.id,
        firstName: dA.firstName,
        lastName: dA.lastName,
        number: dA.forTeamOfficial ? -1 : dA.number,
        rule: dA.rule?.id,
        details: dA.details ?? "",
        game: gameReportId,
        redCardIssued: dA.redCardIssued
    } as DisciplinaryActionDEO
}

export function  checkActionReadyForUpload(dAction: DisciplinaryAction) {
    return dAction.rule != undefined &&
        (dAction.firstName != "" ||
            dAction.lastName != "") &&
        (dAction.number != undefined || dAction.forTeamOfficial)
}

export async function uploadDisciplinaryAction(action: DisciplinaryAction, gameReportId:number):Promise<number> {

    if (checkActionReadyForUpload(action)) {
        let nexturl: string
        if (action.id != undefined) {
            nexturl = `/api/gamereport/disciplinaryAction/update`
        } else {
            nexturl = `/api/gamereport/disciplinaryAction/new`
        }
        return makePostRequest(
            nexturl,
            fromDisciplinaryActionToDisciplinaryActionDEO(action, gameReportId)
        )
            .then(data => parseAndHandleDEO(data, DisciplinaryActionDEO))
            .then(dADEO => dADEO.id)
    } else {
        return -1 //We don't throw an error here, because we always get one empty disciplinary action
    }


}
export function disciplinaryActionIsBlank(disciplinaryAction: DisciplinaryAction) {
    return disciplinaryAction.firstName.trim().length == 0 &&
        disciplinaryAction.lastName.trim().length == 0 &&
        (!disciplinaryAction.number && !disciplinaryAction.forTeamOfficial) &&
        !disciplinaryAction.rule &&
        disciplinaryAction.details.trim().length == 0
}

export async function deleteDisciplinaryActionOnServer(disciplinaryAction:DisciplinaryAction):Promise<boolean> {
    return makePostRequest(
        "/api/gamereport/disciplinaryAction/delete",
        {id: disciplinaryAction.id}
    )
        .then(data => {
            return parseAndHandleDEO(data, z.object({id: z.number()}))
                .then(() => {
                    return true
                })
        })
}
