# Team History — Feature Investigation

**Branch:** `feature/team-history`
**Date:** 2025
**Status:** Investigation / design proposal (no code changes yet)

---

## 1. Goal

Currently a team is nothing more than a row in the `Teams` table. Renames,
conversions to amalgamations and merges overwrite or delete data, so the past
is lost. The goal of this feature is:

1. Track **team name changes** with a **date of change**.
2. Track **amalgamation changes** (members added/removed, team converted to an
   amalgamation, teams merged) with a **date of change**.
3. Keep the **actual history** in the database.
4. **User-facing interactions must not change** (same endpoints, same payloads,
   same UI for referees/CCC/public).
5. The **admin interface** gets a new option to **view a team's history**.

---

## 2. Current state analysis

### 2.1 Database schema (current)

Defined in `src/main/kotlin/eu/gaelicgames/referee/data/ReportData.kt`.

```kotlin
object Teams : LongIdTable() {
    val name = varchar("name", 100)
    val isAmalgamation = bool("is_amalgamation")
}

object Amalgamations : LongIdTable() {
    val amalgamation = reference("amalgamation", Teams)   // the amalgamation team
    val addedTeam     = reference("added_team", Teams)    // a member team
}
```

Facts:
- `Teams` stores **only the current state**: name + amalgamation flag. There are
  **no timestamps at all** (no `created_at`, no `updated_at`), and no
  versioning.
- `Amalgamations` is the current membership list of amalgamation teams. Adding /
  removing members is done by inserting / deleting rows (see
  `TeamDEO.updateInDatabase()`).
- Teams are referenced from many tables via FK to `Teams.id`:
  `GameReports.team_a / team_b`, `DisciplinaryActions.team`, `Injuries.team`,
  `Substitutions.team`, `TournamentReportTeamPreSelections.team`,
  `TournamentTeamPreSelections.team`, `Amalgamations.amalgamation / added_team`.
- **Merge hard-deletes** the merged-away team row (`mergeTeam.delete()` in
  `MergeTeamsDEO.updateInDatabase()`), after re-pointing all FKs to the base
  team. This is the main reason history is lost today.

### 2.2 Schema creation / migration mechanism

`src/main/kotlin/eu/gaelicgames/referee/util/DatabaseUtil.kt`

- `DatabaseHandler.tables` — the full list of Exposed tables, created with
  `SchemaUtils.create(table)`.
- `createSchema()` — after creating all tables, runs a sequence of numbered
  "migrations" using `SchemaUtils.createMissingTablesAndColumns(...)`
  (additive column/table additions). Latest is "Migration 8 - Add Substitutions
  table".
- New history tables/columns must be added here (new table → `tables` list +
  new migration block).

### 2.3 Backend touch points (every place teams are written)

| # | Location | Operation | Writes |
|---|----------|-----------|--------|
| 1 | `RefereeApiRouting.kt` — `post<Api.NewTeam>` | Referee creates a plain team | `Team.new { name; isAmalgamation=false }`; name-uniqueness check |
| 2 | `RefereeApiRouting.kt` — `post<Api.NewAmalgamation>` | Referee creates an amalgamation | `NewAmalgamationDEO.createInDatabase()` → new `Team` + `Amalgamation` rows |
| 3 | `TeamDEO.updateInDatabase()` (called from `AdminApiRouting.kt` — `post<Api.Team.Update>`) | Admin edits team / amalgamation | renames team, toggles `isAmalgamation`, inserts/deletes `Amalgamation` rows |
| 4 | `MergeTeamsDEO.updateInDatabase()` (called from `AdminApiRouting.kt` — `post<Api.Team.Merge>`) | Admin merges teams | re-points all FKs to base team, moves amalgamation memberships, **deletes** merged-away teams |
| 5 | `GenerateFakeData.kt` — `addMockTeams()` | Mock data seeding (`ADD_MOCK_DATA=true`) | `Team.new { ... }` |

Cache: every mutating path already calls `CacheUtil.deleteCachedTeamList()`
(Redis). The public list is served from `TeamDEO.allTeamList()` →
`/api/teams_available` (see `PublicApiRouting.kt`).

### 2.4 Frontend touch points

User-facing (must stay unchanged):

- `components/team/CreateTeam.vue` → `POST /api/new_team` (`NewTeamDEO {name}`)
- `components/team/CreateAmalgamation.vue`, `SmartCreateAmalgamation.vue`,
  `CreateSplitTeam.vue` → `POST /api/new_amalgamation` (`NewAmalgamationDEO {name, teams}`)
- `components/team/MergeTeamDialog.vue` → `POST /api/team/merge` (`MergeTeamsDEO {baseTeam, teamsToMerge}`)
- Team pickers (`TeamSelectField.vue`, `TeamSelector.vue`, `PreselectTeam.vue`,
  `CreateSplitTeam.vue`) → read-only via `loadAllTeams()` / public store.

Admin (Team manager, `admin_router.ts` → `/teams` → `TeamManager.vue`):

- `TeamList.vue` — inline name edit → `POST /api/team/update` (`TeamDEO`)
- `EditAmalgamationDialog.vue` — rename + add/remove members → `POST /api/team/update`
- `ConvertTeamToAmalgamtionDialog.vue` — convert team to amalgamation (1 member)
  → `POST /api/team/update`, or merge into existing squad → `POST /api/team/merge`
- `MergeTeamDialog.vue` — merge → `POST /api/team/merge`

The admin team screen is currently a single `TeamManager.vue` with two
`TeamList.vue` tables (plain teams / amalgamations). A "History" entry fits
naturally as a per-row button + dialog, or as a third view.

### 2.5 DTO layer

Shared DTOs live in the `gaa-referee-report-common` submodule
(`.../data/api/TeamDEOBase.kt`):

```kotlin
data class TeamDEO(name, id, isAmalgamation, amalgamationTeams: List<TeamDEO>?)
data class NewTeamDEO(name)
data class NewAmalgamationDEO(name, teams)
data class MergeTeamsDEO(baseTeam, teamsToMerge)
```

Backend mapping helpers in `src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEO.kt`.
Frontend mirrors them in `frontend-vite/src/types/team_types.ts` + `utils/api/teams_api.ts`.

---

## 3. Interpretation of the requirements

- "Team name changes ... with date of change" → a rename must record **old
  name**, **new name**, and an **effective date** (can be backdated by the
  admin; the user explicitly wants the date of the change, not just the
  timestamp of the edit).
- "Amalgamation changes with date of change" → covering:
  - team converted into an amalgamation (flag `false → true`),
  - member teams added to / removed from an amalgamation,
  - teams merged (the merged-away team's members/membership move into the base).
- "I want the actual history" → history must survive **hard deletes**, i.e. the
  merge operation must no longer destroy the merged-away team's record, or the
  history table must be self-contained. Recommended: soft-delete + history
  table (see §5).
- "All interactions with teams to the user should not change" → the current
  `Teams` table stays the source of truth for the **current state**; all
  existing endpoints and DTO shapes (`TeamDEO`, `NewTeamDEO`,
  `NewAmalgamationDEO`, `MergeTeamsDEO`) stay byte-compatible. History is
  additive (new table + new admin-only endpoint + new admin UI).
- "Only in the admin interface ... see the team history" → history read access
  is admin-scoped (`authenticate("admin-session")`), never exposed to
  referee/CCC/public routes.

---

## 4. Design options considered

### Option A — Append-only event log (RECOMMENDED)

New table `TeamHistoryEvents` (event-sourcing style). `Teams` remains the
current-state table, plus **soft-delete columns** so merged/deleted teams keep
their identity (and keep their history rows referenceable).

```sql
CREATE TABLE team_history_events (
    id            BIGSERIAL PRIMARY KEY,
    team_id       BIGINT NOT NULL REFERENCES teams(id),
    change_type   VARCHAR(40) NOT NULL,      -- CREATED | RENAMED | CONVERTED_TO_AMALGAMATION |
                                             -- MEMBER_ADDED | MEMBER_REMOVED | MERGED_INTO
    change_date   DATE NOT NULL,             -- effective date of the change (user-supplied / backdatable)
    old_value     TEXT NULL,                 -- e.g. old team name, or JSON of removed member ids
    new_value     TEXT NULL,                 -- e.g. new team name, or JSON of added member ids
    recorded_at   TIMESTAMP NOT NULL,        -- server time when the event was written
    recorded_by   BIGINT NULL REFERENCES users(id)   -- admin/referee who triggered it
);
CREATE INDEX idx_team_history_team ON team_history_events(team_id);
CREATE INDEX idx_team_history_date ON team_history_events(change_date);
```

Amalgamation membership can be expressed as `MEMBER_ADDED` / `MEMBER_REMOVED`
events (with old/new values), which keeps a single table and a simple timeline.

Teams soft-delete:

```sql
ALTER TABLE teams ADD COLUMN deleted_at   TIMESTAMP NULL;
ALTER TABLE teams ADD COLUMN merged_into  BIGINT NULL REFERENCES teams(id);
```

- `Teams.name` / `Teams.is_amalgamation` / `Amalgamations` stay exactly as they
  are → **zero impact on existing queries and FKs**.
- Merge: instead of `mergeTeam.delete()`, set `deleted_at` + `merged_into`, and
  write a `MERGED_INTO` event. All FK re-pointing logic stays untouched.
- Timeline rendering in the admin UI is a trivial ordered query per team.
- Point-in-time reconstruction ("what did this team look like on date X") is
  possible by replaying events up to X, but not free.

**Pros:** minimal invasiveness, additive schema, simple admin timeline, keeps
the "current state" model untouched. **Cons:** point-in-time queries require
event replay (acceptable for an admin-only history view).

### Option B — Versioned teams (slowly-changing dimension, type 2)

`Teams` becomes `(identity_id, version, name, is_amalgamation, valid_from,
valid_to)`; `Amalgamations` gets `valid_from/valid_to` on each membership row.

- Powerful for "state at date X" queries, but **invasive**: every FK
  (`GameReports.team_a`, ...) currently targets `Teams.id`; either the id must
  stay the current-version id (then old versions live in a separate version
  table — effectively Option A plus a snapshot table) or all FKs must change to
  identity ids, which breaks the "user-facing interactions unchanged" goal.
- More complex write logic (closing/opening versions on every change).

Verdict: overkill for this feature; not recommended as the primary design.

### Option C — Full snapshots

`TeamSnapshot(snapshot_id, team_id, snapshot_date, payload JSON)` capturing the
whole team + membership state on every change.

- Simplest to implement, great for "show me the state at date X", but
  noisy/duplicative for a simple rename and does not model "name change" or
  "member added" as first-class events. Could be added later on top of
  Option A if point-in-time state views are ever wanted.

---

## 5. Recommended data model (Option A in detail)

### 5.1 New tables / columns (Exposed)

In `ReportData.kt` (or a new `TeamHistory.kt`):

```kotlin
enum class TeamChangeType { CREATED, RENAMED, CONVERTED_TO_AMALGAMATION,
                            MEMBER_ADDED, MEMBER_REMOVED, MERGED_INTO }

object TeamHistoryEvents : LongIdTable() {
    val team       = reference("team_id", Teams)
    val changeType = enumerationByName<TeamChangeType>("change_type", 40)
    val changeDate = date("change_date")                       // effective date
    val oldValue   = text("old_value").nullable()              // old name / JSON member ids
    val newValue   = text("new_value").nullable()              // new name / JSON member ids
    val recordedAt = datetime("recorded_at")
    val recordedBy = optReference("recorded_by", Users)
}

class TeamHistoryEvent(id: EntityID<Long>) : LongEntity(id) { ... }
```

Soft-delete columns on `Teams`:

```kotlin
object Teams : LongIdTable() {
    val name           = varchar("name", 100)
    val isAmalgamation = bool("is_amalgamation")
    val deletedAt      = datetime("deleted_at").nullable()     // NEW
    val mergedInto     = optReference("merged_into", Teams)    // NEW
}
```

- `Teams` in `DatabaseHandler.tables` (already there) — `createMissingTablesAndColumns`
  will add the two new columns; add `TeamHistoryEvents` to the `tables` list and
  a "Migration 9" block in `createSchema()`.
- All existing queries on `Teams` are untouched; only the merge path changes
  (`delete()` → soft-delete) and the public team list must filter
  `deleted_at IS NULL` (see §7.1 — the join query in `TeamDEO.kt`).

### 5.2 Where history is written (single helper)

Add to `TeamDEO.kt` (or a new `TeamHistory.kt`):

```kotlin
suspend fun recordTeamHistory(
    team: Team,
    type: TeamChangeType,
    changeDate: LocalDate,
    oldValue: String? = null,
    newValue: String? = null,
    recordedBy: User? = null
)
```

Call sites:

| Change | Call site | Event(s) written |
|--------|-----------|------------------|
| New plain team | `RefereeApiRouting` `post<Api.NewTeam>` | `CREATED` (name) |
| New amalgamation | `NewAmalgamationDEO.createInDatabase()` | `CREATED` + `MEMBER_ADDED` per member |
| Rename / convert / member add / member remove | `TeamDEO.updateInDatabase()` | diff name → `RENAMED`; `isAmalgamation false→true` → `CONVERTED_TO_AMALGAMATION`; membership diff → `MEMBER_ADDED`/`MEMBER_REMOVED` per member |
| Merge | `MergeTeamsDEO.updateInDatabase()` | on merged-away team: `MERGED_INTO` (old name, target id); on base team: `MEMBER_ADDED` for members that moved over |
| Mock data | `GenerateFakeData.addMockTeams()` | optional `CREATED` events (nice for dev/demo) |

The `change_date` comes from the DTO (admin can backdate); default = today.

### 5.3 New admin-only endpoint

In `resources/Api.kt` (Ktor Resources pattern):

```kotlin
@Serializable @Resource("team")
class Team(val parent: Api) {
    @Serializable @Resource("update") class Update(val parent: Team)
    @Serializable @Resource("merge") class Merge(val parent: Team)
    @Serializable @Resource("history/{id}") class History(val parent: Team, val id: Long)   // NEW
}
```

In `AdminApiRouting.kt` (inside `adminApiRouting()`, which is already wrapped in
`authenticate("admin-session")`):

```kotlin
get<Api.Team.History> { history ->
    val events = lockedTransaction {
        TeamHistoryEvent.find { TeamHistoryEvents.team eq history.id }
            .sortedBy { it.changeDate }
            .map { TeamHistoryEventDEO.from(it) }
    }
    call.respond(events)
}
```

New shared DTO in the common submodule (`TeamDEOBase.kt`):

```kotlin
@Serializable
data class TeamHistoryEventDEO(
    val changeType: String,      // TeamChangeType name
    val changeDate: String,      // ISO date
    val oldValue: String?,
    val newValue: String?,
    val recordedAt: String       // ISO timestamp
)
```

### 5.4 Admin UI

- `TeamList.vue`: add a small "History" button per row (next to
  "Merge with..."/"Convert"/"Edit teams").
- New `TeamHistoryDialog.vue`: `GET /api/team/history/{id}` and render a
  timeline (PrimeVue `Timeline` or a simple ordered list):
  - `CREATED` → "Team was created" (+ date)
  - `RENAMED` → "Renamed from X to Y" (+ date)
  - `CONVERTED_TO_AMALGAMATION` → "Converted to amalgamation" (+ date)
  - `MEMBER_ADDED`/`MEMBER_REMOVED` → "X joined/left the amalgamation" (+ date)
  - `MERGED_INTO` → "Merged into Y" (+ date)
- New `teamHistoryApi` function in `utils/api/teams_api.ts` + zod type in
  `types/team_types.ts`.
- Optional: extend `EditAmalgamationDialog` / `ConvertTeamToAmalgamtionDialog`
  with a date picker ("date of change") that is sent in the update payload so
  backdated renames/amalgamations are recorded correctly.

### 5.5 Backfill / migration of existing data

- Existing teams predate the feature and have no creation date. Recommended
  backfill: one `CREATED` event per existing team with `change_date` set to a
  sensible default (e.g., today or a configured "unknown" date) — or make
  `change_date` nullable and display "unknown".
- Soft-delete: existing data is unaffected (`deleted_at` null).
- No changes to `Amalgamations` needed for history to start; only events going
  forward.

---

## 6. Impact analysis

### 6.1 Unchanged (requirement: user-facing must not change)

- `POST /api/new_team`, `/api/new_amalgamation`, `/api/team/update`,
  `/api/team/merge` — same routes, same request/response DTOs.
- `GET /api/teams_available` — same `TeamDEO[]` shape (after filtering
  soft-deleted teams).
- All referee/CCC/public flows and components.
- All FK-referencing tables (`GameReports`, `Injuries`, `DisciplinaryActions`,
  `Substitutions`, preselections) — untouched.

### 6.2 Changed (minimal, internal)

- `Teams` table: +2 nullable columns (`deleted_at`, `merged_into`).
- `MergeTeamsDEO.updateInDatabase()`: replace hard `delete()` with soft-delete
  + `MERGED_INTO` event (FK re-pointing logic unchanged).
- `TeamDEO.wrapRow/wrapJoinedRow/allTeamList` join queries: add
  `Teams.deletedAt.isNull()` filter so deleted teams disappear from the public
  list (they still appear in admin history).
- `TeamDEO.updateInDatabase()` / `NewAmalgamationDEO.createInDatabase()` /
  `post<Api.NewTeam>`: write history events (no behavior change otherwise).
- `DatabaseUtil.createSchema()`: migration 9 (+ new table in `tables`).
- `GenerateFakeData.addMockTeams()`: optional history seeding.

### 6.3 Tests affected

- `MergeTeamsDEOTest` asserts `Team.findById(duplicateTeamId) == null` after
  merge — must change to "soft-deleted" assertion (`deletedAt != null`).
- New test class `TeamHistoryDEOTest` (pattern: `MergeTeamsDEOTest`,
  `TestHelper.setupDatabase()`), covering:
  - create → `CREATED` event,
  - rename → `RENAMED` with old/new name + date,
  - convert to amalgamation → `CONVERTED_TO_AMALGAMATION` + `MEMBER_ADDED`,
  - member remove → `MEMBER_REMOVED`,
  - merge → base team gets members, merged team soft-deleted + `MERGED_INTO`,
  - public team list excludes soft-deleted teams.

---

## 7. Open questions for the product owner

1. **Who enters the "date of change"?** Proposal: admin can backdate
   (default = today). Is a date required or optional?
2. **Merge date:** when merging, is the merge date the effective date for the
   `MERGED_INTO` event on the old team and the member moves on the base team?
   (Proposal: yes, one date per operation.)
3. **History for merged-away teams:** the old team row must survive (soft
   delete). Confirm that's acceptable — this changes the current hard-delete
   behavior of merges. (Required for "actual history".)
4. **Backfill:** what `change_date` should existing teams get for their
   `CREATED` event?
5. **Should the amalgamation membership timeline also show *when* each member
   joined/left** (i.e., do we need point-in-time membership reconstruction, or
   is the event timeline enough)? If point-in-time is needed, add
   `valid_from/valid_to` columns to `Amalgamations` (Option B-lite) in addition
   to the event log.
6. **Should mock data (`ADD_MOCK_DATA`) write history events** for realistic
   dev/demo data?
7. **Permissions:** admin-only confirmed (not CCC)? CCC currently can only view
   submitted reports; teams history should be admin-only per the requirement.
8. **Uniqueness of names:** currently only checked on create, not on rename.
   Should history also record (and block) duplicate renames? (Out of scope for
   history, but worth deciding.)

---

## 8. Suggested implementation order

1. DB: `TeamHistoryEvents` table + `Teams.deleted_at/merged_into` + migration 9
   (`ReportData.kt`, `DatabaseUtil.kt`).
2. Shared DTOs: `TeamHistoryEventDEO` (+ zod types) in common submodule and
   frontend.
3. Backend: `recordTeamHistory()` helper; wire into `NewTeam`, `NewAmalgamation`,
   `TeamDEO.updateInDatabase()`, `MergeTeamsDEO.updateInDatabase()`; soft-delete
   in merge; filter deleted teams from `allTeamList()`.
4. API: `Api.Team.History` resource + `get` route in `AdminApiRouting.kt`.
5. Frontend: `TeamHistoryDialog.vue` + "History" button in `TeamList.vue` +
   `teams_api.ts` function.
6. Tests: update `MergeTeamsDEOTest`, add `TeamHistoryDEOTest`.
7. Backfill script for existing teams' `CREATED` events (run once at deploy).

---

## 9. Appendix — exact current write paths (for the implementer)

- `src/main/kotlin/eu/gaelicgames/referee/plugins/routing/RefereeApiRouting.kt`
  lines 116–137 (`post<Api.NewTeam>`), 139–151 (`post<Api.NewAmalgamation>`).
- `src/main/kotlin/eu/gaelicgames/referee/plugins/routing/AdminApiRouting.kt`
  lines 142–160 (`post<Api.Team.Update>` at 142, `post<Api.Team.Merge>` at 152).
- `src/main/kotlin/eu/gaelicgames/referee/data/api/TeamDEO.kt`:
  `updateInDatabase()` (line 99), `MergeTeamsDEO.updateInDatabase()` (line
  139), `NewAmalgamationDEO.createInDatabase()` (line 231).
- `src/main/kotlin/eu/gaelicgames/referee/util/GenerateFakeData.kt`
  `addMockTeams()` (line 114).
- Frontend admin: `components/admin/teams/TeamManager.vue`, `TeamList.vue`,
  `EditAmalgamationDialog.vue`, `ConvertTeamToAmalgamtionDialog.vue`;
  `router/admin_router.ts`; `utils/api/teams_api.ts`; `types/team_types.ts`.
