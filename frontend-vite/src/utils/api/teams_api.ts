import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import {
    DeletedTeamAliasDEO,
    MergeTeamsDEO,
    Team,
    TeamAliasDEO,
    TeamHistoryEventDEO
} from "@/types/team_types";


export async function createTeam(name: string, changeDate?: string): Promise<Team> {
    return makePostRequest(
        "/api/new_team",
        {name: name.trim(), changeDate: changeDate}
    )
        .then(data => parseAndHandleDEO(data, Team))
}

export async function loadAllTeams(): Promise<Array<Team>> {
    return fetch("/api/teams_available")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, Team.array()))
}

export async function createAmalgamationOnServer(name: string, teams: Array<Team>, changeDate?: string) {
    return makePostRequest(
        "/api/new_amalgamation",
        {name: name, teams: teams, changeDate: changeDate}
    )
        .then(data => parseAndHandleDEO(data, Team))
}

export async function editTeamOnServer(team: Team, keepOldNameAsAlias?: string): Promise<Team> {
    return makePostRequest("/api/team/update", {
        name: team.name,
        id: team.id,
        isAmalgamation: team.isAmalgamation,
        amalgamationTeams: team.amalgamationTeams ?? null,
        changeDate: team.changeDate,
        keepOldNameAsAlias: keepOldNameAsAlias
    })
        .then(data => parseAndHandleDEO(data, Team))
}

export async function addTeamAlias(teamId: number, alias: string): Promise<TeamAliasDEO> {
    return makePostRequest("/api/team/alias/new", {teamId: teamId, alias: alias.trim()})
        .then(data => parseAndHandleDEO(data, TeamAliasDEO))
}

export async function updateTeamAlias(id: number, alias: string): Promise<TeamAliasDEO> {
    return makePostRequest("/api/team/alias/update", {id: id, alias: alias.trim()})
        .then(data => parseAndHandleDEO(data, TeamAliasDEO))
}

export async function deleteTeamAlias(id: number): Promise<DeletedTeamAliasDEO> {
    return makePostRequest("/api/team/alias/delete", {id: id})
        .then(data => parseAndHandleDEO(data, DeletedTeamAliasDEO))
}

export async function mergeTeamsOnServer(
    baseTeam: Team,
    mergeTeams: Array<Team>,
    changeDate?: string,
    aliasesToCreate?: Record<string, string>
): Promise<Team> {
    const data = MergeTeamsDEO.safeParse({
        baseTeam: baseTeam.id,
        teamsToMerge: mergeTeams.map(value => value.id),
        changeDate: changeDate,
        aliasesToCreate: aliasesToCreate ?? {}
    })
    if (!data.success) {
        return Promise.reject("Could not parse data")
    }
    return makePostRequest(
        "/api/team/merge",
        data.data

    )
        .then(data => parseAndHandleDEO(data, Team))
}

export async function getTeamHistory(teamId: number): Promise<Array<TeamHistoryEventDEO>> {
    return fetch(`/api/team/history/${teamId}`)
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, TeamHistoryEventDEO.array()))
}
