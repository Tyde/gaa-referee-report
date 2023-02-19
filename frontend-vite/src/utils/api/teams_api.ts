import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import {Team} from "@/types/team_types";


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

export async function createAmalgamationOnServer(name:string, teams: Array<Team>) {
    return makePostRequest(
        "/api/new_amalgamation",
        {name: name, teams: teams}
    )
        .then(data => parseAndHandleDEO(data, Team))
}

export async function editTeamOnServer(team: Team): Promise<Team> {
    return makePostRequest("/api/team/update", team)
        .then(data => parseAndHandleDEO(data, Team))
}