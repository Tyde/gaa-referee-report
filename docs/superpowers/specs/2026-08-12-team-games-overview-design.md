# Team Games Overview Design

**Date:** 2026-08-12  
**Status:** Approved

## Goal

Give administrators a dedicated view of every recorded game in a submitted
referee report for a selected team. The view groups games by tournament and
shows the opponent, result, referee, and relevant game metadata.

For a regular team, an administrator may opt into results recorded against
amalgamations that currently contain the selected team. That option is off by
default. It intentionally uses current relationships because historical
relationships generally represent corrected data entry rather than meaningful
past membership.

## Scope

The feature includes:

- Submitted tournament reports only.
- Tournaments containing at least one recorded game for the selected team or,
  when enabled, one of its current amalgamations.
- A dedicated admin page linked from each row of the Teams overview.
- A lightweight, purpose-built backend endpoint and shared DEOs.
- Links from each tournament section to the existing full tournament report.

The feature does not include:

- Draft or unsubmitted referee reports.
- Tournament/report preselections without a recorded game.
- Historical reconstruction of amalgamation membership.
- Expansion from an amalgamation into its member clubs' direct games.
- New database tables, schema migrations, or result caching.

## User Experience

### Entry point and route

Add a `Games` action beside `History` for each row in `TeamList.vue`. The action
navigates to:

`/teams/:teamId/games`

The dedicated page provides a back link to `/teams` and displays the selected
team name as its heading.

### Controls and summary

For a regular team, show an unchecked checkbox labelled `Also show amalgamated
teams`. Enabling it reloads the results and adds games played by every current
amalgamation whose `addedTeam` is the selected team.

Hide the checkbox when the selected entity is itself an amalgamation. Its
direct games are already included, and reverse expansion to member-club games
is outside this feature.

Show compact counts for:

- Recorded games in the current result set.
- Distinct tournaments in the current result set.

### Tournament and game presentation

Group the flat API results by tournament. Order tournament sections newest
first, using `endDate` for leagues when present and `date` otherwise. Each
tournament section displays its date or date range, name, location, and a link
to `/tournament-reports/complete/:tournamentId`.

Order games within a tournament by start time. Null start times sort after
known start times. Each game displays:

- Game date/time when available.
- The actual participating team as `Played as`.
- The opponent.
- Both GAA scores in `goals-points (total)` format.
- Win, draw, or loss from the `Played as` team's perspective.
- Referee name.
- Game code.
- Game type when present.

`Played as` remains visible for direct results so the table layout does not
change when amalgamation results are toggled on.

While loading, display the existing spinner/loading treatment. A valid team
with no games gets an explicit empty-state message. Request failures are sent
to the existing admin error store. When the checkbox changes, a request
sequence guard prevents an older response from replacing the latest response.

## API Design

### Resource

Add an admin-authorized Ktor resource:

`GET /api/team/games/{teamId}?includeAmalgamatedTeams=false`

The route belongs in `AdminApiRouting.kt` and therefore follows the existing
admin session/API-token authentication boundary. The query parameter defaults
to `false`.

### Shared response definitions

Place serializable DEO definitions in the common submodule under
`data/api/*DEOBase.kt`. Keep database imports and Exposed logic out of the
submodule. Put query/mapping functions in the main repository under
`data/api/*.kt`.

The response has this logical shape:

```text
TeamGamesDEO
  selectedTeam: TeamDEO
  includedAmalgamations: List<TeamDEO>
  games: List<TeamGameDEO>

TeamGameDEO
  gameId: Long
  reportId: Long
  tournament: TournamentDEO
  startTime: LocalDateTime?
  playedAsTeam: TeamDEO
  opponentTeam: TeamDEO
  playedAsGoals: Int
  playedAsPoints: Int
  opponentGoals: Int
  opponentPoints: Int
  refereeId: Long
  refereeName: String
  codeId: Long
  codeName: String
  gameTypeId: Long?
  gameTypeName: String?
```

The frontend calculates score totals and W/D/L from the raw goals and points.
Returning an oriented `playedAs`/`opponent` record keeps Team A versus Team B
storage details out of the page.

`includedAmalgamations` is empty when expansion is disabled. When enabled, it
records exactly which current relationships affected the result, making the
response self-describing and allowing later UI improvements without changing
query semantics.

### Query behavior

Within one locked transaction:

1. Find the selected team and reject missing or soft-deleted teams as not
   found.
2. Initialize the included team-ID set with the selected team's ID.
3. If expansion is requested and the selected team is regular, query current
   `Amalgamations` rows where `addedTeam` is the selected team, add their
   `amalgamation` IDs, and return those teams in `includedAmalgamations`.
4. Join `GameReports` to `TournamentReports`, `Tournaments`, `Users`,
   `GameCodes`, and optionally `GameTypes`.
5. Select rows where `teamA` or `teamB` belongs to the included ID set and
   `TournamentReports.isSubmitted` is true.
6. Orient each result around whichever included team appears in that game. If
   both sides somehow belong to the included set, return the game once with
   the selected team taking precedence, otherwise use a deterministic team-A
   orientation.
7. Return an empty `games` list for a valid team with no matches.

No preselection table participates in this query. Current amalgamation rows
are the only source for optional expansion; `TeamHistoryEvents` are not read.

## Frontend Structure

Add:

- `components/admin/teams/TeamGamesPage.vue` for page orchestration and
  presentation.
- Zod schemas mirroring the shared DEOs in `types/team_types.ts`.
- `getTeamGames(teamId, includeAmalgamatedTeams)` in `utils/api/teams_api.ts`.
- The `/teams/:teamId/games` entry in `router/admin_router.ts`.

Keep score formatting, W/D/L calculation, tournament grouping, and ordering
in small pure helpers rather than embedding the logic in the template. This
makes behavior testable and keeps the page component focused on loading state
and rendering.

## Error Handling

- A missing, deleted, or malformed team ID produces the existing structured
  API not-found/error response and does not run the game query.
- A valid team with no results returns HTTP success with an empty games list.
- The frontend sends API and schema-validation failures to
  `adminStore.newError` and retains a non-stale state.
- The checkbox is disabled while its replacement request is active to prevent
  repeated toggles; the request sequence guard still protects against delayed
  network responses.
- A missing optional game start time or game type does not prevent the row from
  rendering.

## Testing and Verification

### Backend tests

Use `TestHelper.setupDatabase()` and `tearDownDatabase()` and cover:

- A selected team stored as Team A is oriented correctly.
- A selected team stored as Team B is oriented correctly.
- Goals, points, opponent, referee, code, game type, report, and tournament
  fields are mapped correctly.
- Unsubmitted reports are excluded.
- Preselected teams with no game are excluded.
- Expansion is disabled by default.
- Enabling expansion includes every amalgamation that currently contains the
  selected team.
- Historical membership events do not affect expansion.
- An amalgamation selection is not expanded into member-club games.
- A game is not duplicated if both sides fall within the included ID set.
- Multiple tournaments and referees are returned.
- A valid team without games returns an empty list.
- Missing and soft-deleted team IDs produce an error.

### Frontend tests

Use Vitest to cover the extracted pure helpers:

- GAA total calculation and `goals-points (total)` formatting.
- Win, draw, and loss classification.
- Grouping games into distinct tournaments.
- League and non-league tournament ordering.
- Game ordering, including null start times.

The repository does not currently include a Vue component-mounting test
library. Do not add one solely for this feature. Verify the default unchecked
checkbox, refetch on toggle, empty state, loading state, and hidden checkbox
for amalgamations through the production build plus a focused manual browser
check during implementation.

### Completion checks

Run:

- The focused backend test class for the new query.
- `./gradlew test`.
- `npm run test:unit` in `frontend-vite/`.
- `npm run lint` in `frontend-vite/`.
- `npm run build` in `frontend-vite/`.

The generated frontend assets are build output and should only be committed if
that is already required by the repository's established workflow.
