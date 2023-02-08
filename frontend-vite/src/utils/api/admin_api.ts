
import {PitchPropertyDEO, Rule} from "@/types";
import type {PitchProperty} from '@/types'
import {ApiError} from "@/types";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import {z} from "zod";



export async function updatePitchVariableOnServer(option: PitchProperty) {
    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify(option)
    };
    const response = await fetch("/api/pitchProperty/update", requestOptions)
    const data = await response.json()
    const parseResult = PitchPropertyDEO.safeParse(data)
    if (parseResult.success) {
        console.log("Pitch property updated")
        return parseResult.data
    } else {
        const apiErrorParse = ApiError.safeParse(data)
        if (apiErrorParse.success) {
            return Promise.reject(apiErrorParse.data.message)
        } else {
            return Promise.reject("Unknown error")
        }
    }
}

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
        .then(data => parseAndHandleDEO(data, z.object({id: z.number()})))
}