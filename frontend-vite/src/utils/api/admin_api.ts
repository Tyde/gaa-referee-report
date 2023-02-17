
import {GameType, NewRuleDEO, Referee, Rule} from "@/types";
import type { NewReferee} from '@/types'
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import {z} from "zod";




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

export async function getAllUsers():Promise<Array<Referee>> {
    return fetch("/api/user/all")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, z.array(Referee)))
}

export async function updateUserOnServer(user: Referee) {
    return makePostRequest("/api/user/update", user)
        .then(data => parseAndHandleDEO(data, Referee))
}

export async function addRefereeOnServer(user: NewReferee) {
    return makePostRequest("/api/user/new", user)
        .then(data => parseAndHandleDEO(data, Referee))
}

export async function updateGameTypeOnServer(gameType: GameType) {
    return makePostRequest("/api/gametype/update", gameType)
        .then(data => parseAndHandleDEO(data, GameType))
}