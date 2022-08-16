import type {GameReport} from "@/types";

export const fromDateToDateString = (date:Date|undefined) => {
    if (date) {
        const offset = date.getTimezoneOffset()
        date = new Date(date.getTime() - (offset*60*1000))
        return date.toISOString().split('T')[0]
    } else {
        return ""
    }
}

export function checkGameReportMinimal(gameReport:GameReport): boolean {
    return gameReport!=undefined && gameReport.teamAReport?.team != undefined &&
        gameReport.teamBReport?.team != undefined &&
        gameReport.startTime != undefined
}
export function checkGameReportNecessary(gameReport:GameReport): boolean {
    return checkGameReportMinimal(gameReport) &&
        gameReport.gameType != undefined &&
        gameReport.extraTime != undefined
}
export function checkGameReportSuggestion(gameReport:GameReport): boolean {
    let sumGoals = (gameReport.teamBReport.goals ?? 0) + (gameReport.teamAReport.goals ?? 0)
    let sumPoints = (gameReport.teamBReport.points ?? 0) + (gameReport.teamAReport.points ?? 0)
    return checkGameReportMinimal(gameReport) &&
        sumGoals + sumPoints > 0
}

