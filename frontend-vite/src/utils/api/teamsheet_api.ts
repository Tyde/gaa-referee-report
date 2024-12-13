import type {FileUploadUploadEvent} from "primevue/fileupload";
import {TeamsheetUploadSuccessDEO, TeamsheetWithClubAndTournamentDataDEO} from "@/types/teamsheet_types";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";

export async function onTeamsheetUploadComplete(event: FileUploadUploadEvent) :
    Promise<TeamsheetUploadSuccessDEO> {
    try {
        const response = JSON.parse(event.xhr.response)
        return parseAndHandleDEO(response, TeamsheetUploadSuccessDEO)
    } catch (e) {
        return Promise.reject(e)
    }
}


export async function loadTeamsheetPlayersFromFileKey(fileKey: string):Promise<TeamsheetUploadSuccessDEO> {
    const data = {fileKey: fileKey}
    return makePostRequest("/api/teamsheet/get_players", data)
        .then(data => parseAndHandleDEO(data, TeamsheetUploadSuccessDEO))
}

export async function setTeamsheetMetaData(data: TeamsheetWithClubAndTournamentDataDEO):Promise<TeamsheetWithClubAndTournamentDataDEO> {
    return makePostRequest("/api/teamsheet/set_metadata", data)
        .then(data => parseAndHandleDEO(data, TeamsheetWithClubAndTournamentDataDEO))
}

export async function getTeamsheetMetaData(fileKey: string):Promise<TeamsheetWithClubAndTournamentDataDEO> {
    const payload = {fileKey: fileKey}
    return makePostRequest("/api/teamsheet/get_metadata", payload)
        .then(data => parseAndHandleDEO(data, TeamsheetWithClubAndTournamentDataDEO))
}


export async function editTeamsheetMetaData(data: TeamsheetWithClubAndTournamentDataDEO):Promise<TeamsheetWithClubAndTournamentDataDEO> {
    return makePostRequest("/api/teamsheet/edit_metadata", data)
        .then(data => parseAndHandleDEO(data, TeamsheetWithClubAndTournamentDataDEO))
}

export async function replaceTeamsheetFileKey(oldfileKey: string, newTeamsheetData: TeamsheetWithClubAndTournamentDataDEO):Promise<TeamsheetWithClubAndTournamentDataDEO> {
    const payload = {oldfileKey: oldfileKey, newTeamsheetData: newTeamsheetData}
    return makePostRequest("/api/teamsheet/replace_file", payload)
        .then(data => parseAndHandleDEO(data, TeamsheetWithClubAndTournamentDataDEO))
}
