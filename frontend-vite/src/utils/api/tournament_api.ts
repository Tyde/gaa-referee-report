import type {DateTime} from "luxon";
import {makePostRequest, parseAndHandleDEO} from "@/utils/api/api_utils";
import {
    DatabaseTournament, PublicTournamentListDEO,
    RegionDEO,
    Tournament,
    TournamentTeamPreselectionDEO,
    tournamentToTournamentDAO
} from "@/types/tournament_types";
import {CompleteTournamentReportDEO, PublicTournamentReportDEO} from "@/types/complete_tournament_types";
import type {Team} from "@/types/team_types";


export async function loadTournamentsOnDate(date:DateTime):Promise<Array<DatabaseTournament>> {
    let dateString = date.toISODate()
    return fetch("/api/tournament/find_by_date/"+dateString)
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, DatabaseTournament.array()))

}

export async function loadAllTournaments():Promise<Array<DatabaseTournament>> {
    return fetch("/api/tournament/all")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, DatabaseTournament.array()))
}

export async function uploadNewTournament(tournament:Tournament):Promise<DatabaseTournament> {
    if(tournament.region && tournament.name && tournament.location && tournament.date) {
        let dataWithCorrectDate = tournamentToTournamentDAO(tournament)
        return makePostRequest("/api/tournament/new", dataWithCorrectDate)
            .then(data => parseAndHandleDEO(data, DatabaseTournament))

    } else {
        return Promise.reject("Missing required fields")
    }
}

export async function loadAllRegions():Promise<Array<RegionDEO>> {
    return fetch("/api/region/all")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, RegionDEO.array()))
}

export async function loadPublicTournamentReport(id:number):Promise<PublicTournamentReportDEO> {
    return fetch("/api/tournament/complete_report_public/"+id)
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, PublicTournamentReportDEO))
}

export async function loadCompleteTournamentReport(id:number):Promise<CompleteTournamentReportDEO> {
    return fetch("/api/tournament/complete_report/"+id)
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, CompleteTournamentReportDEO))
}

export async function loadTournamentPreselectedTeams(id: number):Promise<TournamentTeamPreselectionDEO> {
    return makePostRequest(
        "/api/tournament/preselected_teams/get",
        {tournamentId: id}
    ).then(data => parseAndHandleDEO(data, TournamentTeamPreselectionDEO))
}


export async function addTournamentPreselectedTeams(tournament: DatabaseTournament, teams: Team[]):Promise<TournamentTeamPreselectionDEO> {
    return makePostRequest(
        "/api/tournament/preselected_teams/add_teams",
        {
            tournamentId: tournament.id,
            teamIds: teams.map(team => team.id)
        } as TournamentTeamPreselectionDEO
    ).then(data => parseAndHandleDEO(data, TournamentTeamPreselectionDEO))
}

export async function setTournamentPreselectedTeams(tournament: DatabaseTournament, teams: Team[]):Promise<TournamentTeamPreselectionDEO> {
    return makePostRequest(
        "/api/tournament/preselected_teams/set_teams",
        {
            tournamentId: tournament.id,
            teamIds: teams.map(team => team.id)
        } as TournamentTeamPreselectionDEO
    ).then(data => parseAndHandleDEO(data, TournamentTeamPreselectionDEO))
}


export async function loadAllTournamentsWithTeams():Promise<PublicTournamentListDEO> {
    return fetch("/api/tournament/all_with_teams")
        .then(response => response.json())
        .then(data => parseAndHandleDEO(data, PublicTournamentListDEO))
}
