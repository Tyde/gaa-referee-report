import {Referee, SessionInfo} from "@/types";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";


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
        .then(data => parseAndHandleDEO(data, SessionInfo))
}