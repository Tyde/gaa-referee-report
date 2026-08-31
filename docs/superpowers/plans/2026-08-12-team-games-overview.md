# Team Games Overview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an admin-only team games page that lists results and referees from submitted reports, with optional expansion through current amalgamation relationships.

**Architecture:** Introduce a compact shared API contract and a single backend query that orients each game as the participating team versus its opponent. The Vue admin page consumes that flat response, applies presentation-only grouping and sorting through tested pure helpers, and links back to the existing complete tournament reports.

**Tech Stack:** Kotlin 1.9.22, Ktor Resources, kotlinx.serialization, Exposed ORM, JUnit 5, Vue 3 Composition API, TypeScript, Zod, PrimeVue, Vue Router, Luxon, Vitest.

## Global Constraints

- Work on the existing `feature/team-games-overview` branch in the main repository.
- Keep serializable DEO definitions in `gaa-referee-report-common`; keep Exposed/database dependencies in the main repository.
- Because `gaa-referee-report-common` is a git submodule, commit its changes in the submodule and then commit the updated gitlink in the main repository.
- Include submitted tournament reports only.
- Include tournaments only when they contain a recorded matching game.
- The amalgamation option is unchecked by default and resolves current `Amalgamations` relationships whose amalgamation team is not soft-deleted.
- Do not reconstruct membership from `TeamHistoryEvents`.
- Do not expand a selected amalgamation into its member clubs' direct games.
- Add no database migration, cache, or new frontend component-testing dependency.
- Preserve unrelated worktree changes, including the existing untracked `opencode.json`.
- Follow TDD within each task: demonstrate a focused failure, make the smallest scoped change, and rerun the focused verification before committing.

## File and Responsibility Map

| Area | File | Responsibility |
|---|---|---|
| Shared contract | `gaa-referee-report-common/src/main/kotlin/eu/gaelicgames/referee/data/api/TeamGameDEOBase.kt` | One oriented game result record |
| Shared contract | `gaa-referee-report-common/src/main/kotlin/eu/gaelicgames/referee/data/api/TeamGamesDEOBase.kt` | Selected-team metadata, included amalgamations, and game collection |
| Ktor resource | `src/main/kotlin/eu/gaelicgames/referee/resources/Api.kt` | Typed admin GET resource and query parameter |
| Backend query | `src/main/kotlin/eu/gaelicgames/referee/data/api/TeamGamesDEO.kt` | Validation, relationship expansion, joined query, and orientation |
| Backend route | `src/main/kotlin/eu/gaelicgames/referee/plugins/routing/AdminApiRouting.kt` | Invoke the query and translate success/failure to the API response |
| Backend tests | `src/test/kotlin/eu/gaelicgames/referee/data/api/TeamGamesDEOTest.kt` | Query semantics and response mapping |
| Frontend schemas | `frontend-vite/src/types/team_types.ts` | Zod/runtime representation of both response DEOs |
| Frontend API | `frontend-vite/src/utils/api/teams_api.ts` | Fetch and validate the team games response |
| View helpers | `frontend-vite/src/utils/team_games.ts` | Score, outcome, grouping, and ordering logic |
| Helper tests | `frontend-vite/src/utils/__tests__/team_games.spec.ts` | Deterministic view-helper behavior |
| Admin page | `frontend-vite/src/components/admin/teams/TeamGamesPage.vue` | Route-driven loading, controls, states, and tournament/game rendering |
| Team entry point | `frontend-vite/src/components/admin/teams/TeamList.vue` | Navigate from a team row to its games page |
| Admin router | `frontend-vite/src/router/admin_router.ts` | Register `/teams/:teamId/games` |

## Dependency and Delegation Order

1. Task 1 establishes the shared contract and route shape and must complete first.
2. After Task 1, Task 2 (backend behavior) and Task 3 (frontend data/helpers) have independent file ownership and may be delegated in parallel.
3. Task 4 depends on Task 3's frontend interfaces; it may proceed independently of Task 2 once the contract is fixed.
4. Task 5 starts only after Tasks 2–4 are integrated.

---

### Task 1: Freeze the Shared API Contract

**Files:**

- Create: `gaa-referee-report-common/src/main/kotlin/eu/gaelicgames/referee/data/api/TeamGameDEOBase.kt`
- Create: `gaa-referee-report-common/src/main/kotlin/eu/gaelicgames/referee/data/api/TeamGamesDEOBase.kt`
- Modify: `src/main/kotlin/eu/gaelicgames/referee/resources/Api.kt`

**Produces:**

- Serializable `TeamGameDEO` with the exact fields approved in the design: game/report identifiers; tournament; optional start time; played-as and opponent teams; both raw goals/points; referee identity/name; game-code identity/name; and optional game-type identity/name.
- Serializable `TeamGamesDEO` containing `selectedTeam`, `includedAmalgamations`, and `games`.
- Typed `Api.Team.Games` GET resource for `/api/team/games/{teamId}` with `includeAmalgamatedTeams: Boolean = false`.

**Requirements:**

- [ ] Initialize and inspect the submodule state before editing; place submodule work on a suitable feature branch without changing unrelated submodule content.
- [ ] Add one top-level DEO per new common file and reuse the project's existing `LocalDateTimeSerializer` for optional game start time.
- [ ] Use existing `TeamDEO` and `TournamentDEO` types rather than duplicating team or tournament fields.
- [ ] Add the Ktor resource beneath `Api.Team` so it inherits the established `/api/team/...` structure.
- [ ] Compile the backend to confirm that the main source set consumes the new common definitions.
- [ ] Commit the common-submodule change, then commit the main-repository resource and submodule pointer as a coherent contract milestone.

**Verification:**

- Run `./gradlew compileKotlin` from the repository root.
- Expected result: the shared types and typed resource compile without database or frontend changes.

---

### Task 2: Build and Test the Backend Team-Games Query

**Files:**

- Create: `src/main/kotlin/eu/gaelicgames/referee/data/api/TeamGamesDEO.kt`
- Create: `src/test/kotlin/eu/gaelicgames/referee/data/api/TeamGamesDEOTest.kt`
- Modify: `src/main/kotlin/eu/gaelicgames/referee/plugins/routing/AdminApiRouting.kt`

**Consumes:**

- `TeamGameDEO`, `TeamGamesDEO`, and `Api.Team.Games` from Task 1.

**Produces:**

- `TeamGamesDEO.Companion.forTeam(teamId: Long, includeAmalgamatedTeams: Boolean): Result<TeamGamesDEO>` as the route-facing query boundary.
- An admin-authorized GET handler that returns the DEO or the repository's structured not-found/error response.

**Requirements:**

- [ ] Create database fixtures through `TestHelper.setupDatabase()` covering teams, amalgamations, tournaments, reports, referees, codes, game types, and games.
- [ ] First establish failing tests for Team A orientation, Team B orientation, raw score mapping, referee/report/tournament metadata, and optional start time/game type.
- [ ] Add failing coverage showing that unsubmitted reports and preselections without games are excluded.
- [ ] Add failing coverage showing direct-only default results and opt-in inclusion of every non-deleted current amalgamation containing the selected regular team.
- [ ] Prove that history events do not influence expansion and that selecting an amalgamation does not add member-club direct games.
- [ ] Cover valid empty results, missing teams, soft-deleted teams, multiple tournaments/referees, and the defensive no-duplicate rule when both game sides are in the included ID set.
- [ ] Implement the query inside one `lockedTransaction`, using a set of effective team IDs and one joined game query rather than per-game database lookups.
- [ ] Orient results around the matched participating team; if both sides match, prefer the explicitly selected team and otherwise use deterministic Team A orientation.
- [ ] Keep outcome calculation and display ordering out of the backend; return raw data only.
- [ ] Wire the Ktor handler through `AdminApiRouting.kt` without changing authentication registration or public/referee routes.
- [ ] Run focused and full backend verification, then commit the backend milestone.

**Verification:**

- Run `./gradlew test --tests "eu.gaelicgames.referee.data.api.TeamGamesDEOTest"`.
- Run `./gradlew test`.
- Expected result: all new query cases and the existing backend suite pass.

---

### Task 3: Add Frontend Contracts, API Client, and Pure View Logic

**Files:**

- Modify: `frontend-vite/src/types/team_types.ts`
- Modify: `frontend-vite/src/utils/api/teams_api.ts`
- Create: `frontend-vite/src/utils/team_games.ts`
- Create: `frontend-vite/src/utils/__tests__/team_games.spec.ts`

**Consumes:**

- The JSON field names and nullability defined by Task 1.

**Produces:**

- Zod schemas/types `TeamGameDEO` and `TeamGamesDEO`.
- `getTeamGames(teamId: number, includeAmalgamatedTeams: boolean): Promise<TeamGamesDEO>`.
- Pure helpers for GAA totals/formatting, played-as outcome, tournament grouping, tournament ordering, and within-tournament game ordering.
- A grouped frontend type suitable for direct iteration by `TeamGamesPage.vue`.

**Requirements:**

- [ ] Mirror every backend response field and its optionality in Zod; validate the complete response before returning it to callers.
- [ ] Encode the checkbox state as the `includeAmalgamatedTeams` query parameter and handle non-success HTTP responses consistently with existing API utilities.
- [ ] Write failing Vitest cases for `goals * 3 + points`, `goals-points (total)` formatting, and win/draw/loss from the played-as perspective.
- [ ] Write failing Vitest cases for grouping by tournament ID, newest-first tournament ordering, league `endDate` precedence, chronological ascending game start-time ordering, and null start times sorting last.
- [ ] Keep helpers independent of Vue, Pinia, router state, and network calls.
- [ ] Run the focused Vitest file and TypeScript checking, then commit the frontend data/helper milestone.

**Verification:**

- Run `npm run test:unit -- src/utils/__tests__/team_games.spec.ts` from `frontend-vite/`.
- Run `npm run type-check` from `frontend-vite/`.
- Expected result: helper tests pass and frontend contracts type-check against their consumers.

---

### Task 4: Build the Dedicated Admin Page and Navigation

**Files:**

- Create: `frontend-vite/src/components/admin/teams/TeamGamesPage.vue`
- Modify: `frontend-vite/src/components/admin/teams/TeamList.vue`
- Modify: `frontend-vite/src/router/admin_router.ts`

**Consumes:**

- `getTeamGames(...)`, the Zod-derived types, and grouping/score/outcome helpers from Task 3.
- Existing full tournament report route `/tournament-reports/complete/:id`.

**Produces:**

- Admin route `/teams/:teamId/games`.
- Per-team `Games` navigation action.
- Complete dedicated results page with loading, error, empty, direct, and expanded states.

**Requirements:**

- [ ] Add the new route with route-prop handling consistent with the existing admin report pages.
- [ ] Add `Games` beside `History` for both regular-team and amalgamation rows without disturbing edit, merge, convert, alias, or history actions.
- [ ] Load direct results on entry and expose an unchecked `Also show amalgamated teams` checkbox only for regular teams.
- [ ] Disable the checkbox during its replacement request and use a monotonically increasing request token so a stale response cannot overwrite current state.
- [ ] Display the selected-team heading, back navigation, game count, and distinct-tournament count.
- [ ] Render grouped tournament sections newest first, including league date ranges, name, location, and navigation to the existing full tournament report.
- [ ] Render each game with date/time when present, `Played as`, opponent, both formatted scores, W/D/L, referee, code, and optional game type.
- [ ] Use existing PrimeVue/Tailwind patterns and the admin error store; do not introduce a new UI or state-management dependency.
- [ ] Show an explicit no-games message for successful empty responses and preserve a stable page shell during loading.
- [ ] Complete a production build and a focused manual browser check, then commit the page/navigation milestone.

**Manual acceptance scenarios:**

- A regular team opens with only direct submitted games and an unchecked checkbox.
- Enabling the checkbox adds current-amalgamation games and makes `Played as` unambiguous.
- An amalgamation opens with direct games and no expansion checkbox.
- A team without recorded submitted games shows the empty state.
- A tournament link opens the existing complete report.
- Rapid checkbox interaction never leaves results inconsistent with the visible control state.

**Verification:**

- Run `npm run type-check` from `frontend-vite/`.
- Run `npm run build` from `frontend-vite/`.
- Run the admin UI locally with representative mock/test data and execute every manual acceptance scenario above.
- Expected result: the route builds, all states render, and existing team actions remain operational.

---

### Task 5: Integrate, Regress, and Prepare Handoff

**Files:**

- Modify only files required to correct integration failures found by this task.
- Update: `docs/superpowers/plans/2026-08-12-team-games-overview-progress.md` if the implementation workflow uses a persistent progress record.

**Consumes:**

- Completed and reviewed outputs of Tasks 1–4.

**Produces:**

- A verified feature branch ready for final code review and integration.

**Requirements:**

- [ ] Confirm the common submodule commit is reachable on its feature branch and the main repository points to that exact commit.
- [ ] Review the combined diff for the approved scope, DEO dependency boundaries, route authorization, query count, stale-response handling, and unrelated generated/static assets.
- [ ] Run backend focused tests and the full backend suite from a clean task state.
- [ ] Run frontend unit tests, type checking, lint, and the production build.
- [ ] Because `npm run lint` uses `--fix`, inspect its diff and retain only relevant formatting changes.
- [ ] Repeat the manual browser acceptance scenarios against the integrated backend endpoint.
- [ ] Confirm `opencode.json` and all unrelated user changes remain unstaged and unmodified.
- [ ] Request code review using `superpowers:requesting-code-review`; resolve findings with focused reruns.
- [ ] Use `superpowers:verification-before-completion` before reporting the branch as complete.
- [ ] Commit only genuine integration corrections; do not squash the task-level milestones unless the user requests it.

**Final verification:**

- `./gradlew test --tests "eu.gaelicgames.referee.data.api.TeamGamesDEOTest"`
- `./gradlew test`
- `npm run test:unit` in `frontend-vite/`
- `npm run type-check` in `frontend-vite/`
- `npm run lint` in `frontend-vite/`, followed by diff inspection
- `npm run build` in `frontend-vite/`
- `git status --short` in both the main repository and common submodule

Expected result: every command succeeds, manual acceptance matches the spec,
the submodule pointer is coherent, and only intentional feature changes are
tracked.
