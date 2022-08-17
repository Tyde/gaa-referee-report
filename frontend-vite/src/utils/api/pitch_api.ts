import {z} from "zod";
import type {Pitch, PitchProperty, Report} from "@/types";
import {ApiError, PitchPropertyType} from "@/types";

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
    let res = await fetch("/api/pitch_variables")
    let json = await res.json()
    let parseResult = PitchVariablesDEO.safeParse(json)
    if (parseResult.success) {
        console.log("Parsed pitch variables")
        let variables = pitchVariablesDEOtoPitchVariables(parseResult.data)
        console.log(variables)
        return variables
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

const PitchDEO = z.object({
    id: z.number().nullable().optional(),
    report: z.number(),
    name: z.string(),
    surface: z.number().nullable().optional(),
    length: z.number().nullable().optional(),
    width: z.number().nullable().optional(),
    smallSquareMarkings: z.number().nullable().optional(),
    penaltySquareMarkings: z.number().nullable().optional(),
    thirteenMeterMarkings: z.number().nullable().optional(),
    twentyMeterMarkings: z.number().nullable().optional(),
    longMeterMarkings: z.number().nullable().optional(),
    goalPosts: z.number().nullable().optional(),
    goalDimensions: z.number().nullable().optional(),
    additionalInformation: z.string().nullable().optional()
})
type PitchDEO = z.infer<typeof PitchDEO>;

export function pitchDEOtoPitch(
    pitchDEO:PitchDEO,
    report: Report,
    pitchVariables:PitchVariables
):Pitch {

    let surfaceValue = pitchVariables.surfaces.find(p => p.id == pitchDEO.surface)
    let widthValue = pitchVariables.widths.find(p => p.id == pitchDEO.width)
    console.log("sdwd width")
    console.log(widthValue)
    let lengthValue = pitchVariables.lengths.find(p => p.id == pitchDEO.length)
    let penaltyValue = pitchVariables.markingsOptions.find(p => p.id == pitchDEO.penaltySquareMarkings)
    let smallSquareValue = pitchVariables.markingsOptions.find(p => p.id == pitchDEO.smallSquareMarkings)
    let thirteenMeterValue = pitchVariables.markingsOptions.find(p => p.id == pitchDEO.thirteenMeterMarkings)
    let twentyMeterValue = pitchVariables.markingsOptions.find(p => p.id == pitchDEO.twentyMeterMarkings)
    let longMeterValue = pitchVariables.markingsOptions.find(p => p.id == pitchDEO.longMeterMarkings)
    let goalPostsValue = pitchVariables.goalPosts.find(p => p.id == pitchDEO.goalPosts)
    let goalDimensionsValue = pitchVariables.goalDimensions.find(p => p.id == pitchDEO.goalDimensions)
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

export async function uploadPitch(pitch:Pitch) {
    if(checkPitchReadForUpload(pitch)) {
        let data = {
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
        console.log(data)
        let requestOpions =  {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(PitchDEO.parse(data))
        }
        let nexturl = ""
        if (pitch.id != undefined) {
            nexturl = `/api/pitch/update`
        } else {
            nexturl = `/api/pitch/new`
        }
        const response = await fetch(nexturl, requestOpions)
        const res = await response.json()

        let parseResult = PitchDEO.safeParse(res)
        if (parseResult.success) {
            console.log("Parsed pitch")
            console.log(parseResult.data)
            if(pitch.id == undefined && parseResult.data.id) {
                pitch.id = parseResult.data.id
            }
            return parseResult.data
        } else {
            return Promise.reject(parseResult.error)
        }
    }

}