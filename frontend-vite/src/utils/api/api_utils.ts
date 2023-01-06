import type {ZodType, ZodTypeAny} from "zod";
import {ApiError} from "@/types";

export async function makePostRequest(url: string, data: any) {
    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify(data)
    };
    const response = await fetch(url, requestOptions)
    return await response.json()
}

export async function parseAndHandleDEO<T>(jsonData:any, deo:ZodType<T>):Promise<T> {
    const parseResult = deo.safeParse(jsonData)
    if (parseResult.success) {
        return parseResult.data
    } else {
        const epR = ApiError.safeParse(jsonData)
        if (epR.success) {
            return Promise.reject(epR.data.message)
        } else {
            return Promise.reject(epR.error.message)
        }
    }
}