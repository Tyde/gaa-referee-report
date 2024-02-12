import {z} from "zod";
import {DeletionResponse} from "@/types";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import type {Report} from "@/types/report_types";
import type {Pitch, PitchProperty, PitchVariables} from "@/types/pitch_types";
import {PitchDEO, PitchPropertyType, PitchPropertyWithTypeDEO, PitchVariablesDEO} from "@/types/pitch_types";


function pitchVariablesDEOtoPitchVariables(pV:PitchVariablesDEO):PitchVariables {
    return {
        surfaces: pV.surfaces.map(p => ({id: p.id, name: p.name, disabled:p.disabled, type: PitchPropertyType.surface})),
        lengths: pV.lengths.map(p => ({id: p.id, name: p.name,disabled:p.disabled, type: PitchPropertyType.length})),
        widths: pV.widths.map(p => ({id: p.id, name: p.name,disabled:p.disabled, type: PitchPropertyType.width})),
        markingsOptions: pV.markingsOptions.map(p => ({id: p.id, name: p.name, disabled:p.disabled, type: PitchPropertyType.markingsOptions})),
        goalPosts: pV.goalPosts.map(p => ({id: p.id, name: p.name,disabled:p.disabled, type: PitchPropertyType.goalPosts})),
        goalDimensions: pV.goalDimensions.map(p => ({id: p.id, name: p.name,disabled:p.disabled, type: PitchPropertyType.goalDimensions})),
    }
}

export async function getPitchVariables():Promise<PitchVariables> {
    return fetch("/api/pitch_variables")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, PitchVariablesDEO))
        .then(pitchVariablesDEOtoPitchVariables)
}

export function checkPitchReadForUpload(pitch:Pitch):boolean {
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
        let nexturl: string
        if (pitch.id != undefined) {
            nexturl = `/api/pitch/update`
        } else {
            nexturl = `/api/pitch/new`
        }
        return makePostRequest(
            nexturl,
            pitchToPitchDEO(pitch)
        )
            .then(data => parseAndHandleDEO(data, PitchDEO))
            .then(pitchDEO => {
                    if(pitch.id === undefined && pitchDEO.id) {
                        pitch.id = pitchDEO.id
                    }
                    return pitchDEO
                }
            )
    } else {
        return Promise.reject(`Pitch "name: ${pitch.name},id: ${pitch.id}" not ready for upload`)
    }

}

export async function deletePitchOnServer(pitch:Pitch):Promise<boolean> {
    return makePostRequest(
        `/api/pitch/delete`,
        {id: pitch.id}
    )
        .then(data => parseAndHandleDEO(data, z.object({id: z.number()})))
        .then(data => data.id == pitch.id)
}

export async function deletePitchPropertyOnServer(pitchProperty:PitchProperty):Promise<DeletionResponse> {
    return makePostRequest(
        `/api/pitch_property/delete`,
        pitchProperty
    )
        .then(data => parseAndHandleDEO(data, DeletionResponse))

}

export async function enablePitchPropertyOnServer(pitchProperty:PitchProperty):Promise<PitchPropertyWithTypeDEO> {
    return makePostRequest(
        `/api/pitch_property/enable`,
        pitchProperty
    )
        .then(data => parseAndHandleDEO(data, PitchPropertyWithTypeDEO))

}

export async function updatePitchPropertyOnServer(pitchProperty:PitchProperty):Promise<PitchPropertyWithTypeDEO> {

    return makePostRequest(
        `/api/pitch_property/update`,
        pitchProperty
    )
        .then(data => parseAndHandleDEO(data, PitchPropertyWithTypeDEO))
        .then(pitchPropertyDEO => {
            if(pitchPropertyDEO.id) {
                return pitchPropertyDEO
            } else {
                return Promise.reject("Server did not respond with id while updating pitch property")
            }
        })
}

export async function createPitchPropertyOnServer(pitchProperty:PitchProperty):Promise<PitchPropertyWithTypeDEO> {
    let data = PitchPropertyWithTypeDEO.omit({id:true}).parse(pitchProperty)
    return makePostRequest(`api/pitch_property/new`, data)
        .then(data => parseAndHandleDEO(data, PitchPropertyWithTypeDEO))
}
