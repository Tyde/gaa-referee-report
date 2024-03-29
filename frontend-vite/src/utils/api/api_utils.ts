import type {z} from "zod";
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

//export async function parseAndHandleDEO<T>(jsonData: any, deo: ZodType<T>): Promise<T> {
export async function parseAndHandleDEO<T extends z.ZodTypeAny>(jsonData: any, deo: T, getApiErrorOnFail: boolean = false): Promise<z.infer<T>> {
    const parseResult = deo.safeParse(jsonData)
    if (parseResult.success) {
        return parseResult.data
    } else {
        const epR = ApiError.safeParse(jsonData)
        if (epR.success) {
            if(getApiErrorOnFail) {
                return Promise.reject(epR.data)
            } else {

                return Promise.reject(epR.data.message)
            }
        } else {
            console.trace()
            return Promise.reject("Trying to parse "+ deo+"error: "+ parseResult.error.message)
        }
    }
}

export function delay(ms: number) {
    return new Promise( resolve => setTimeout(resolve, ms) );
}

