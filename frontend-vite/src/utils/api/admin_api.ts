
import {PitchPropertyDEO} from "@/types";
import type {PitchProperty} from '@/types'
import {ApiError} from "@/types";



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