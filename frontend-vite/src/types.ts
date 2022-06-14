export default interface Team {
    name: string,
    id: number,
    isAmalgamation: boolean
    amalgamationTeams?: Team[]
}