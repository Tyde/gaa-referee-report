import {DateTime} from "luxon";

export interface Team {
    name: string,
    id: number,
    isAmalgamation: boolean
    amalgamationTeams?: Team[]
}

export interface Tournament {
    name: string
    location: string
    date: Date
}

export interface DatabaseTournament extends Tournament{
    id: number
}

export interface Report {
    tournament:DatabaseTournament
    selectedTeams:Array<Team>
    gameCode:GameCode
    id?:number
}

export interface GameCode {
    id: number,
    name:string
}

export interface GameType {
    id: number,
    name:string
}

export interface ExtraTimeOption {
    id: number,
    name:string
}

export interface GameReport {
    id?:number
    report: Report,
    startTime?: DateTime,
    gameType?: GameType,
    teamAReport: SingleTeamGameReport,
    teamBReport: SingleTeamGameReport,
    extraTime?: ExtraTimeOption,
    umpirePresentOnTime: boolean,
    umpireNotes:string
}

export interface Injury {
    id?: number,
    firstName: string,
    lastName: string,
    details: string,
}

export interface Rule {
    id: number,
    code: number,
    isCaution: boolean,
    isBlack: boolean,
    isRed: boolean,
    description: string
}

export interface DisciplinaryAction {
    id?: number,
    team?: Team,
    firstName: string,
    lastName: string,
    number?: number,
    rule?: Rule,
    details: string,
}

export interface SingleTeamGameReport {
    team?: Team,
    goals?: number,
    points?: number,
    injuries: Injury[],
    disciplinaryActions: DisciplinaryAction[],
}