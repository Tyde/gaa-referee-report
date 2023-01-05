import {z} from "zod";
import type {Pitch, PitchProperty, Report} from "@/types";
import {ApiError, PitchDEO, PitchPropertyDEO, PitchPropertyType} from "@/types";

const PitchProperyDEO = z.object({
    id: z.number(),
    name: z.string().min(1),
})
type PitchProperyDEO = z.infer<typeof PitchProperyDEO>
const PitchVariablesDEO = z.object({
    surfaces: PitchProperyDEO.array(),
    lengths: PitchProperyDEO.array(),
    widths: PitchProperyDEO.array(),
    markingsOptions: PitchProperyDEO.array(),
    goalPosts: PitchProperyDEO.array(),
    goalDimensions: PitchProperyDEO.array()
})
type PitchVariablesDEO = z.infer<typeof PitchVariablesDEO>;

export interface PitchVariables {
    surfaces: Array<PitchProperty>,
    lengths: Array<PitchProperty>,
    widths: Array<PitchProperty>,
    markingsOptions: Array<PitchProperty>,
    goalPosts: Array<PitchProperty>,
    goalDimensions: Array<PitchProperty>,
}

function pitchVariablesDEOtoPitchVariables(pV:PitchVariablesDEO):PitchVariables {
    return {
        surfaces: pV.surfaces.map(p => ({id: p.id, name: p.name, type: PitchPropertyType.surface})),
        lengths: pV.lengths.map(p => ({id: p.id, name: p.name, type: PitchPropertyType.length})),
        widths: pV.widths.map(p => ({id: p.id, name: p.name, type: PitchPropertyType.width})),
        markingsOptions: pV.markingsOptions.map(p => ({id: p.id, name: p.name, type: PitchPropertyType.markingsOptions})),
        goalPosts: pV.goalPosts.map(p => ({id: p.id, name: p.name, type: PitchPropertyType.goalPosts})),
        goalDimensions: pV.goalDimensions.map(p => ({id: p.id, name: p.name, type: PitchPropertyType.goalDimensions})),
    }
}

export async function getPitchVariables():Promise<PitchVariables> {
    const res = await fetch("/api/pitch_variables")
    const json = await res.json()
    const parseResult = PitchVariablesDEO.safeParse(json)
    if (parseResult.success) {
        return pitchVariablesDEOtoPitchVariables(parseResult.data)
    } else {
        const errorParse = ApiError.safeParse(json)
        if (errorParse.success) {
            return Promise.reject(errorParse.data.message)
        }
        return Promise.reject(parseResult.error)
    }

}

function checkPitchReadForUpload(pitch:Pitch):boolean {
    return pitch.name!=undefined && pitch.name.trim().length > 0
}



export function pitchDEOtoPitch(
    pitchDEO:PitchDEO,
    report: Report,
    pitchVariables:PitchVariables
):Pitch {

    const surfaceValue = pitchVariables.surfaces.find(p => p.id == pitchDEO.surface)
    const widthValue = pitchVariables.widths.find(p => p.id == pitchDEO.width)
    const lengthValue = pitchVariables.lengths.find(p => p.id == pitchDEO.length)
    const penaltyValue = pitchVariables.markingsOptions.find(p => p.id == pitchDEO.penaltySquareMarkings)
    const smallSquareValue = pitchVariables.markingsOptions.find(p => p.id == pitchDEO.smallSquareMarkings)
    const thirteenMeterValue = pitchVariables.markingsOptions.find(p => p.id == pitchDEO.thirteenMeterMarkings)
    const twentyMeterValue = pitchVariables.markingsOptions.find(p => p.id == pitchDEO.twentyMeterMarkings)
    const longMeterValue = pitchVariables.markingsOptions.find(p => p.id == pitchDEO.longMeterMarkings)
    const goalPostsValue = pitchVariables.goalPosts.find(p => p.id == pitchDEO.goalPosts)
    const goalDimensionsValue = pitchVariables.goalDimensions.find(p => p.id == pitchDEO.goalDimensions)
    return <Pitch>{
        id: pitchDEO.id,
        report: report,
        name: pitchDEO.name,
        surface: surfaceValue,
        length: lengthValue,
        width: widthValue,
        smallSquareMarkings: smallSquareValue,
        penaltySquareMarkings: penaltyValue,
        thirteenMeterMarkings: thirteenMeterValue,
        twentyMeterMarkings: twentyMeterValue,
        longMeterMarkings: longMeterValue,
        goalPosts: goalPostsValue,
        goalDimensions: goalDimensionsValue,
        additionalInformation: pitchDEO.additionalInformation
    }
}

function pitchToPitchDEO(pitch: Pitch):PitchDEO {
    const data = {
        id: pitch.id,
        report: pitch.report.id,
        name: pitch.name,
        surface: pitch.surface?.id,
        length: pitch.length?.id,
        width: pitch.width?.id,
        smallSquareMarkings: pitch.smallSquareMarkings?.id,
        penaltySquareMarkings: pitch.penaltySquareMarkings?.id,
        thirteenMeterMarkings: pitch.thirteenMeterMarkings?.id,
        twentyMeterMarkings: pitch.twentyMeterMarkings?.id,
        longMeterMarkings: pitch.longMeterMarkings?.id,
        goalPosts: pitch.goalPosts?.id,
        goalDimensions: pitch.goalDimensions?.id,
        additionalInformation: pitch.additionalInformation
    }
    return PitchDEO.parse(data);
}

export async function uploadPitch(pitch:Pitch) {
    if(checkPitchReadForUpload(pitch)) {
        const data = pitchToPitchDEO(pitch);
        const requestOpions =  {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        }
        let nexturl = ""
        if (pitch.id != undefined) {
            nexturl = `/api/pitch/update`
        } else {
            nexturl = `/api/pitch/new`
        }
        const response = await fetch(nexturl, requestOpions)
        const res = await response.json()

        const parseResult = PitchDEO.safeParse(res)
        if (parseResult.success) {
            if(pitch.id == undefined && parseResult.data.id) {
                pitch.id = parseResult.data.id
            }
            return parseResult.data
        } else {
            return Promise.reject(parseResult.error)
        }
    }

}

export async function deletePitchOnServer(pitch:Pitch):Promise<boolean> {
    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify({id: pitch.id})
    };
    const response = await fetch("/api/pitch/delete", requestOptions)
    const data = await response.json()
    const parseResult = z.object({
        id: z.number(),
    }).safeParse(data)
    if (parseResult.success) {
        return true
    } else {
        const epR = ApiError.safeParse(data)
        if (epR.success) {
            return Promise.reject(epR.data.message)
        } else {
            return Promise.reject("Unknown error")
        }
    }
}

export async function deletePitchPropertyOnServer(pitchProperty:PitchProperty):Promise<boolean> {
    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify({id: pitchProperty.id})
    };
    const response = await fetch("/api/pitch_property/delete", requestOptions)
    const data = await response.json()
    const parseResult = z.object({
        id: z.number(),
    }).safeParse(data)
    if (parseResult.success) {
        return true
    } else {
        const epR = ApiError.safeParse(data)
        if (epR.success) {
            return Promise.reject(epR.data.message)
        } else {
            return Promise.reject("Unknown error")
        }
    }
}

export async function updatePitchPropertyOnServer(pitchProperty:PitchProperty):Promise<number> {
    const requestOptions = {
        method: "POST",
        headers: {
            'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify(pitchProperty)
    };
    const response = await fetch("/api/pitch_property/update", requestOptions)
    const data = await response.json()
    const parseResult = PitchPropertyDEO.safeParse(data)
    if (parseResult.success) {
        if (parseResult.data.id) {
            return parseResult.data.id
        } else {
            return Promise.reject("Server did not respond with id while updating pitch property")
        }
    } else {
        const epR = ApiError.safeParse(data)
        if (epR.success) {
            return Promise.reject(epR.data.message)
        } else {
            return Promise.reject("Unknown error")
        }
    }
}