import type {DisciplinaryAction, GameReport, Injury} from "@/types/game_report_types";
import {injuryIsBlank} from "@/utils/api/injuries_api";
import {disciplinaryActionIsBlank} from "@/utils/api/disciplinary_action_api";

export enum InjuryIssue {
    NoName,
    NoDetails
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
    NoTeamA,
    NoTeamB,
    NoScores,
    InjuriesIncomplete,
    DisciplinaryActionsIncomplete,
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

export class GameReportIssues {
    issues: Array<GameReportIssue> = []
    disciplinaryActionIssues = new Array<DisciplinaryActionIssues>()
    injuriesIssues = new Array<InjuriesIssues>()
    gameReport: GameReport

    constructor(gameReport: GameReport, issues: Array<GameReportIssue>, disciplinaryActionIssues: Array<DisciplinaryActionIssues>, injuriesIssues: Array<InjuriesIssues>) {
        this.gameReport = gameReport
        this.issues = issues
        this.disciplinaryActionIssues = disciplinaryActionIssues
        this.injuriesIssues = injuriesIssues
    }
}

export function injuryIssuesForGameReport(gameReport: GameReport): Array<InjuriesIssues> {
    return gameReport.teamAReport.injuries.concat(
        gameReport.teamBReport.injuries
    ).map((injury) => {
        if (!injuryIsBlank(injury)) {
            let fullName = injury.firstName + " " + injury.lastName
            let issues: Array<InjuryIssue> = []
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
        let issues: Array<DisciplinaryActionIssue> = []
        let fullName = disciplinaryAction.firstName + " " + disciplinaryAction.lastName
        if (!disciplinaryActionIsBlank(disciplinaryAction)) {
            if (fullName.trim().length == 0) {
                issues.push(DisciplinaryActionIssue.NoName)
            }
            if (disciplinaryAction.number === undefined) {
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

