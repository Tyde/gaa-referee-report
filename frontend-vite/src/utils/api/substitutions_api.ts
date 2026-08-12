import {z} from "zod";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import type {Team} from "@/types/team_types";
import {SubstitutionDEO} from "@/types/game_report_types";
import type {Substitution} from "@/types/game_report_types";

function substitutionToSubstitutionDEO(substitution: Substitution, gameReportId: number) {
    return {
        id: substitution.id,
        team: substitution.team?.id,
        playerOnFirstName: substitution.playerOnFirstName,
        playerOnLastName: substitution.playerOnLastName,
        playerOnNumber: substitution.playerOnNumber,
        playerOffFirstName: substitution.playerOffFirstName,
        playerOffLastName: substitution.playerOffLastName,
        playerOffNumber: substitution.playerOffNumber,
        minute: substitution.minute,
        game: gameReportId
    } as SubstitutionDEO
}

export function substitutionDEOToSubstitution(substitutionDEO: SubstitutionDEO, team: Team): Substitution {
    return {
        id: substitutionDEO.id,
        playerOnFirstName: substitutionDEO.playerOnFirstName,
        playerOnLastName: substitutionDEO.playerOnLastName,
        playerOnNumber: substitutionDEO.playerOnNumber,
        playerOffFirstName: substitutionDEO.playerOffFirstName,
        playerOffLastName: substitutionDEO.playerOffLastName,
        playerOffNumber: substitutionDEO.playerOffNumber,
        minute: substitutionDEO.minute,
        team: team
    } as Substitution
}

function checkSubstitutionReadyForUpload(substitution: Substitution) {
    const playerOnHasName = (substitution.playerOnFirstName.trim() + substitution.playerOnLastName.trim()).length > 0
    const playerOffHasName = (substitution.playerOffFirstName.trim() + substitution.playerOffLastName.trim()).length > 0
    return substitution.team != undefined &&
        substitution.playerOnNumber != undefined &&
        substitution.playerOffNumber != undefined &&
        substitution.minute != undefined &&
        playerOnHasName &&
        playerOffHasName
}

export async function uploadSubstitution(substitution: Substitution, gameReportId: number): Promise<number> {
    if (checkSubstitutionReadyForUpload(substitution)) {
        let nexturl: string
        if (substitution.id != undefined) {
            nexturl = `/api/gamereport/substitution/update`
        } else {
            nexturl = `/api/gamereport/substitution/new`
        }
        return makePostRequest(
            nexturl,
            substitutionToSubstitutionDEO(substitution, gameReportId)
        )
            .then((data) => parseAndHandleDEO(data, SubstitutionDEO))
            .then((substitutionDEO) => substitutionDEO.id)
    } else {
        return -1
    }
}

export async function deleteSubstitutionOnServer(substitution: Substitution): Promise<boolean> {
    if (substitution.id != undefined) {
        return makePostRequest(
            `/api/gamereport/substitution/delete`,
            {id: substitution.id}
        )
            .then((data) => parseAndHandleDEO(data, z.object({id: z.number()})))
            .then((data) => data.id == substitution.id)
    }
    return Promise.reject("Trying to delete a substitution on the server that has not been saved to the server")
}

export function substitutionIsBlank(substitution: Substitution) {
    return substitution.playerOnFirstName.trim().length == 0 &&
        substitution.playerOnLastName.trim().length == 0 &&
        substitution.playerOffFirstName.trim().length == 0 &&
        substitution.playerOffLastName.trim().length == 0 &&
        substitution.playerOnNumber == undefined &&
        substitution.playerOffNumber == undefined &&
        substitution.minute == undefined
}
