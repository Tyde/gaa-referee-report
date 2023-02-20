import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import {
    Referee,
    RefereeWithRoleDEO, SetRefereeRoleDEO,
    UpdateRefereeDAO,
    UpdateRefereePasswordDAO,
    UpdateRefereePasswordResponse
} from "@/types/referee_types";


export async function validateActivationToken(token: string): Promise<Referee> {
    return makePostRequest("/api/user/validate_activation_token", {token: token})
        .then(data => parseAndHandleDEO(data, Referee))
}

export async function activateUser(token: string, password: string) {
    return makePostRequest("/api/user/activate", {token: token, password: password})
        .then(data => parseAndHandleDEO(data, Referee))
}

export async function getSessionInfo() {
    return fetch("/api/session")
        .then(data => data.json())
        .then(data => parseAndHandleDEO(data, RefereeWithRoleDEO))
}

export async function updateUserRole(update: SetRefereeRoleDEO) {
    return makePostRequest("/api/user/set_role", update)
        .then(data => parseAndHandleDEO(data, RefereeWithRoleDEO))
}

export async function updateMeUser(changedValues: UpdateRefereeDAO) {
    let val = UpdateRefereeDAO.parse(changedValues)
    return makePostRequest("/api/user/update_me", val)
        .then(data => parseAndHandleDEO(data, Referee))
}

export async function updatePasswordOnServer(update:UpdateRefereePasswordDAO) {
    return makePostRequest("/api/user/update_password", update)
        .then(data => parseAndHandleDEO(data, UpdateRefereePasswordResponse))
}

