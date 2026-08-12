import type {DisciplinaryAction, GameReport, Injury, Substitution} from "@/types/game_report_types";
import {injuryIsBlank} from "@/utils/api/injuries_api";
import {disciplinaryActionIsBlank} from "@/utils/api/disciplinary_action_api";
import {substitutionIsBlank} from "@/utils/api/substitutions_api";

export enum InjuryIssue {
    NoName,
    NoDetails
}

export enum SubstitutionIssue {
    NoPlayerOnName,
    NoPlayerOnNumber,
    NoPlayerOffName,
    NoPlayerOffNumber,
    NoMinute
}

export enum PitchReportIssue {
    NoName,
    NoSurface,
    MissingDimension,
    MarkingsIncomplete,
    GoalInfoIncomplete,
}

export enum GameReportIssue {
    NoGameType,
    NoStartingTime,
    NoExtraTimeOption,
    NoGameLengthOption,
    NoTeamA,
    NoTeamB,
    NoScores,
    InjuriesIncomplete,
    DisciplinaryActionsIncomplete,
    SubstitutionsIncomplete,
    TeamAEqualTeamB,
}


export enum DisciplinaryActionIssue {
    NoRule,
    NoName,
    NoNumber
}

export class DisciplinaryActionIssues {
    issues: Array<DisciplinaryActionIssue>
    action: DisciplinaryAction

    constructor(issues: Array<DisciplinaryActionIssue>, action: DisciplinaryAction) {
        this.issues = issues
        this.action = action
    }
}

export class InjuriesIssues {
    issues: Array<InjuryIssue>
    action: Injury

    constructor(issues: Array<InjuryIssue>, action: Injury) {
        this.issues = issues
        this.action = action
    }
}

export class SubstitutionsIssues {
    issues: Array<SubstitutionIssue>
    action: Substitution

    constructor(issues: Array<SubstitutionIssue>, action: Substitution) {
        this.issues = issues
        this.action = action
    }
}

export class GameReportIssues {
    issues: Array<GameReportIssue> = []
    disciplinaryActionIssues = new Array<DisciplinaryActionIssues>()
    injuriesIssues = new Array<InjuriesIssues>()
    substitutionsIssues = new Array<SubstitutionsIssues>()
    gameReport: GameReport

    constructor(gameReport: GameReport, issues: Array<GameReportIssue>, disciplinaryActionIssues: Array<DisciplinaryActionIssues>, injuriesIssues: Array<InjuriesIssues>, substitutionsIssues: Array<SubstitutionsIssues> = []) {
        this.gameReport = gameReport
        this.issues = issues
        this.disciplinaryActionIssues = disciplinaryActionIssues
        this.injuriesIssues = injuriesIssues
        this.substitutionsIssues = substitutionsIssues
    }
}

export function injuryIssuesForGameReport(gameReport: GameReport): Array<InjuriesIssues> {
    return gameReport.teamAReport.injuries.concat(
        gameReport.teamBReport.injuries
    ).map((injury) => {
        if (!injuryIsBlank(injury)) {
            const fullName = injury.firstName + " " + injury.lastName
            const issues: Array<InjuryIssue> = []
            if (fullName.trim().length == 0) {
                issues.push(InjuryIssue.NoName)
            }
            if (injury.details.trim().length == 0) {
                issues.push(InjuryIssue.NoDetails)
            }
            return new InjuriesIssues(issues, injury)
        } else {
            return undefined
        }
    }).filter((ii) => (ii?.issues.length || 0) > 0)
        .filter((ii): ii is InjuriesIssues => (!!ii))
}


export function disciplinaryActionIssuesForGameReport(gameReport: GameReport): Array<DisciplinaryActionIssues> {
    return gameReport.teamAReport.disciplinaryActions.concat(
        gameReport.teamBReport.disciplinaryActions
    ).map((disciplinaryAction) => {
        const issues: Array<DisciplinaryActionIssue> = []
        const fullName = disciplinaryAction.firstName + " " + disciplinaryAction.lastName
        if (!disciplinaryActionIsBlank(disciplinaryAction)) {
            if (fullName.trim().length == 0) {
                issues.push(DisciplinaryActionIssue.NoName)
            }
            if (disciplinaryAction.number === undefined && !disciplinaryAction.forTeamOfficial) {
                issues.push(DisciplinaryActionIssue.NoNumber)
            }
            if (disciplinaryAction.rule === undefined) {
                issues.push(DisciplinaryActionIssue.NoRule)
            }
            return new DisciplinaryActionIssues(issues, disciplinaryAction)
        } else {
            return undefined
        }
    }).filter((dai) => (dai?.issues.length || 0) > 0)
        .filter((dai): dai is DisciplinaryActionIssues => !!dai)
}


export function substitutionIssuesForGameReport(gameReport: GameReport): Array<SubstitutionsIssues> {
    return gameReport.teamAReport.substitutions.concat(
        gameReport.teamBReport.substitutions
    ).map((substitution) => {
        if (!substitutionIsBlank(substitution)) {
            const issues: Array<SubstitutionIssue> = []
            const playerOnName = substitution.playerOnFirstName + " " + substitution.playerOnLastName
            const playerOffName = substitution.playerOffFirstName + " " + substitution.playerOffLastName
            if (playerOnName.trim().length == 0) {
                issues.push(SubstitutionIssue.NoPlayerOnName)
            }
            if (substitution.playerOnNumber === undefined) {
                issues.push(SubstitutionIssue.NoPlayerOnNumber)
            }
            if (playerOffName.trim().length == 0) {
                issues.push(SubstitutionIssue.NoPlayerOffName)
            }
            if (substitution.playerOffNumber === undefined) {
                issues.push(SubstitutionIssue.NoPlayerOffNumber)
            }
            if (substitution.minute === undefined) {
                issues.push(SubstitutionIssue.NoMinute)
            }
            return new SubstitutionsIssues(issues, substitution)
        } else {
            return undefined
        }
    }).filter((si) => (si?.issues.length || 0) > 0)
        .filter((si): si is SubstitutionsIssues => !!si)
}
