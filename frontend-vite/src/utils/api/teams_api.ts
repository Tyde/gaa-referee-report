import {ApiError, NewTeamDEO, Team} from "@/types";
import {z} from "zod";



export async function createTeam(name: string): Promise<Team> {
    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify({"name": name.trim()})
    };
    const response = await fetch("/api/new_team", requestOptions)
    const data = await response.json()
    const parseResult = Team.safeParse(data)
    if (parseResult.success) {
        return parseResult.data
    } else {
        const errorParse = ApiError.safeParse(data)
        if(errorParse.success) {
            return Promise.reject(errorParse.data.message)
        }
        return Promise.reject("Error parsing response")
    }
}

export async function allTeams(): Promise<Array<Team>> {
    const response = await fetch("/api/teams_available")
    const data = await response.json()
    const parsed = Team.array().safeParse(data)
    if (parsed.success) {
        return parsed.data
    } else {
        return Promise.reject("Error parsing teams")
    }
}