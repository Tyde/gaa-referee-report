
import {GameType} from "@/types";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import {z} from "zod";
import {Referee, RefereeWithRoleDEO, UpdateRefereePasswordResponse} from "@/types/referee_types";
import type {NewUser} from "@/types/referee_types";
import {NewRuleDEO, Rule} from "@/types/rules_types";
import {DatabaseTournament, databaseTournamentToTournamentDAO} from "@/types/tournament_types";




export async function updateRuleOnServer(rule: Rule) {
    return makePostRequest("/api/rule/update",rule)
        .then(data => parseAndHandleDEO(data,Rule))
}

export async function checkIfRuleDeletable(rule: Rule):Promise<boolean> {
    return makePostRequest("/api/rule/check_deletable", {id: rule.id})
        .then(data => parseAndHandleDEO(data, z.object({id: z.number(),isDeletable: z.boolean()})))
        .then(data => data.isDeletable)
}

export async function deleteRuleOnServer(rule: Rule) {
    return makePostRequest("/api/rule/delete", {id: rule.id})
        .then(data => parseAndHandleDEO(data, z.object({id: z.number()})))
}

export async function toggleRuleStateOnServer(rule: Rule) {
    return makePostRequest("/api/rule/disable", {id: rule.id})
        .then(data => parseAndHandleDEO(data, Rule))
}

export async function addRuleOnServer(rule: NewRuleDEO) {
    return makePostRequest("/api/rule/new", rule)
        .then(data => parseAndHandleDEO(data, Rule))
}

export async function getAllUsers():Promise<Array<RefereeWithRoleDEO>> {
    return fetch("/api/user/all")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, z.array(RefereeWithRoleDEO)))
}

export async function updateUserOnServer(user: Referee) {
    return makePostRequest("/api/user/update", user)
        .then(data => parseAndHandleDEO(data, Referee))
}

export async function addUserOnServer(user: NewUser) {
    return makePostRequest("/api/user/new", user)
        .then(data => parseAndHandleDEO(data, Referee))
}

export async function resetRefereePasswordOnServer(user: Referee) {
    return makePostRequest("/api/user/reset_password", {id: user.id})
        .then(data => parseAndHandleDEO(data, UpdateRefereePasswordResponse))
}

export async function updateGameTypeOnServer(gameType: GameType) {
    return makePostRequest("/api/gametype/update", gameType)
        .then(data => parseAndHandleDEO(data, GameType))
}

export async function updateTournamentOnServer(tournament: DatabaseTournament) {
    let data = databaseTournamentToTournamentDAO(tournament)
    return makePostRequest("/api/tournament/update", data)
        .then(data => parseAndHandleDEO(data, DatabaseTournament))
}

export async function deleteTournamentOnServer(tournament: DatabaseTournament) {
    let data = {id: tournament.id}
    return makePostRequest("api/tournament/delete", data)
        .then(data => parseAndHandleDEO(data, z.object({id: z.number()})))
}
