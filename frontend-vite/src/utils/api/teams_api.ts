import {ApiError, Team} from "@/types";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";


export async function createTeam(name: string): Promise<Team> {
    return makePostRequest(
        "/api/new_team",
        {name: name.trim()}
    )
        .then(data => parseAndHandleDEO(data, Team))
}

export async function allTeams(): Promise<Array<Team>> {
    return fetch("/api/teams_available")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, Team.array()))
}