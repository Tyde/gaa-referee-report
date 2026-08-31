# Team Name Aliases — Design

**Date:** 2026-08-11
**Status:** Approved design, ready for implementation planning

## Problem

The user base is international, so the same club is known by different spellings:
Zurich / Zürich, "Gaélique Bro Sant Brieg" / "Saint-Brieuc". A `Team` row carries a
single `name` (`Teams.name`, `src/main/kotlin/eu/gaelicgames/referee/data/ReportData.kt:17`),
so a referee searching with their own spelling finds nothing and creates a duplicate
team instead. Duplicates then have to be merged by hand, and the next referee with the
same expectation creates the duplicate again.

The client-side search already normalizes diacritics
(`normalizeForSearch`, `frontend-vite/src/components/team/TeamSelectField.vue:140`),
which covers Zürich → Zurich but nothing else. Genuinely different names — translations,
local-language forms, historic names — are unreachable.

## Goal

A team can be found under any spelling a user reasonably expects, without changing what
anyone reads, and without opening a path for referees to pollute team data.

## Decisions

These were settled during brainstorming and constrain everything below.

1. **Canonical display only.** Aliases are search keys. Reports, PDFs, stats, public
   tables and ELO tables always render `Teams.name`. No locale-tagged variants, no
   per-report spellings. Blast radius on rendering: zero.
2. **Admin-only writes.** Only admins create, edit or delete aliases. Referees never
   submit spellings.
3. **Create-team flow untouched.** No "did you mean" confirmation step before creating a
   team. Duplicates are still handled by admin merge; the alias set is what stops the
   *next* duplicate.
4. **Merge offers an alias.** Merging a team proposes the merged-away team's name as an
   alias of the survivor. The admin can deny it or edit the text before committing.
5. **Rename offers an alias.** Renaming a team proposes the old name as an alias. Same
   control: admin can deny it or edit the text first.
6. **Canonical name is never stored as an alias.** A new team has zero aliases. Search
   matches `Teams.name` directly, so there is nothing to keep in sync and a rename
   leaves no stale row behind.

## Data model

New table in `ReportData.kt`, next to `Teams`:

```kotlin
object TeamAliases : LongIdTable() {
    val team = reference("team", Teams)
    val alias = varchar("alias", 100)
    val normalized = varchar("normalized", 100).uniqueIndex()
    val createdAt = datetime("created_at")
    val createdBy = optReference("created_by", Users)
}

class TeamAlias(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<TeamAlias>(TeamAliases)
    var team by Team referencedOn TeamAliases.team
    var alias by TeamAliases.alias
    var normalized by TeamAliases.normalized
    var createdAt by TeamAliases.createdAt
    var createdBy by User optionalReferencedOn TeamAliases.createdBy
}
```

- `alias` is what an admin typed and what the UI shows. `normalized` is the derived
  search/uniqueness key: lowercased, diacritics stripped, using the same rules as
  `normalizeForSearch`.
- The unique index on `normalized` is **global**, not per team: a spelling must resolve to
  exactly one team or search is ambiguous. This is a second unique constraint alongside
  the `LongIdTable` primary key, which is normal — one PRIMARY KEY, any number of UNIQUE
  indexes.
- Insert additionally rejects an alias whose normalized form equals any team's canonical
  name (including its own team's).
- Aliases attach to `Teams` rows, so amalgamations and squads get the feature for free.

### Normalization is shared

`normalized` is derived data. The normalizer must exist in exactly one place, used by
both backend writes and frontend matching, and any change to its rules ships with a
migration that recomputes every `TeamAliases.normalized` value and reports collisions
introduced by the new rules.

### History

Two new `TeamChangeType` values: `ALIAS_ADDED`, `ALIAS_REMOVED`. Alias changes are
recorded through the existing `TeamHistoryEvents` table and appear in
`TeamHistoryDialog.vue` with no new audit machinery. `oldValue` / `newValue` carry the
alias text.

### Merge interaction

When a team that owns aliases is itself merged into another team, its aliases are
re-pointed to the surviving team. Aliases that would collide with an existing alias or
with the survivor's canonical name are dropped, not failed on.

## API

`gaa-referee-report-common/.../TeamDEOBase.kt`, `src/.../api/TeamDEO.kt`,
`AdminApiRouting.kt`.

- `TeamDEO` gains `val aliases: List<TeamAliasDEO>? = null`, where
  `TeamAliasDEO(id: Long, teamId: Long, alias: String)`. Objects rather than bare strings,
  because the admin UI needs alias ids to edit and delete individual rows. Nullable with a
  default keeps the wire format compatible, so `mcp-server` and the kottster admin keep
  compiling unchanged.
- Aliases are attached to top-level teams only. Amalgamation *member* teams inside
  `amalgamationTeams` carry no aliases; members are already matched by canonical name.
- New admin-only DEOs and routes:
  - `NewTeamAliasDEO(teamId: Long, alias: String)`
  - `UpdateTeamAliasDEO(id: Long, alias: String)`
  - `DeleteTeamAliasDEO(id: Long)`
- `MergeTeamsDEO` gains `aliasesToCreate: Map<Long, String>` — merged-team-id to alias
  text. A missing entry means the admin denied that alias. Applied inside the same
  transaction as the merge (`TeamDEO.kt:240`), with `ALIAS_ADDED` history events.
- Team edit takes a dedicated write DEO (`UpdateTeamDEO`) carrying
  `keepOldNameAsAlias: String?` — `null` means don't create one. This keeps the read-side
  `TeamDEO` free of write-only fields. Applied in the same transaction as the rename in
  `TeamDEO.updateInDatabase`, next to the existing `RENAMED` history event
  (`TeamDEO.kt:206`).
- Every alias mutation calls `CacheUtil.deleteCachedTeamList()`. The team list is cached
  (`TeamDEO.kt:137`); a stale cache would silently break alias search.

### Alias validation errors are not fatal to their operation

A rejected alias (collision, empty after trim) must not fail the merge or the rename that
proposed it. The primary operation commits; the alias failure is reported back per row.

## Search

Matching stays **client-side**. The full team list is already loaded into the store and
aliases ride along in the same payload, so there is no new search endpoint and no
round-trip per keystroke.

`filtered_list` (`TeamSelectField.vue:163`) currently matches a normalized substring
against `value.name` and amalgamation member names, and hardcodes `search_score` to `1`.
Extend it to aliases and start using the score:

| Match | Score |
|---|---|
| canonical name, exact | 100 |
| canonical name, prefix | 80 |
| canonical name, substring | 60 |
| alias, exact | 50 |
| alias, prefix | 40 |
| alias, substring | 30 |
| amalgamation member name | 20 |

Sort by score descending, then by name. Canonical always outranks alias, so familiar
results stay on top and aliases only fill in what would otherwise be an empty list.

When a row matched **only** via an alias, show a subtitle naming the matched spelling,
e.g. `matched "Saint-Brieuc"`. Without it a referee types their spelling, sees an
unfamiliar canonical name, doesn't recognise it, and creates a duplicate anyway — the
exact failure this feature exists to prevent.

Fix `:key="srt.search_score"` (`TeamSelectField.vue:309`) to `srt.team.id`. It is a
duplicate key for every row today and becomes a real render bug once scores vary.

## Admin UI

**`admin/teams/TeamList.vue`** — per-team alias editor: existing aliases as removable
chips plus an add field. Collision errors are shown inline and name the owner
("already an alias of Zürich").

**`team/MergeTeamDialog.vue`** — for each team in `teamsToMerge`
(`MergeTeamDialog.vue:32`), one row with a "keep name as alias" checkbox (default on) and
an editable text field prefilled with that team's name. Per-row validation feedback on
submit.

**Rename UI** — three components call `editTeamOnServer`: `TeamList.vue`,
`EditAmalgamationDialog.vue` and `ConvertTeamToAmalgamtionDialog.vue`. Wherever the name
field can change, show the same control: a "keep old name as alias" checkbox (default on)
with the old name prefilled and editable, appearing only once the name actually differs.

## Backfill

Migration 10 in `DatabaseUtil.kt`, following the pattern established at
`DatabaseUtil.kt:148`:

1. `SchemaUtils.createMissingTablesAndColumns(TeamAliases)`
2. For every `Teams` row with `mergedInto != null`, walk the `mergedInto` chain to the
   surviving team and insert the dead team's name as an alias of it. Skip on collision,
   and skip when it normalizes to the survivor's own canonical name. Write `ALIAS_ADDED`
   history events.

This makes the feature useful immediately rather than whenever an admin finds time: every
duplicate already merged becomes a working search term.

Renames are **not** backfilled from `TeamHistoryEvents`, even though old names are
recorded there. Historic renames include typo corrections, and enshrining those as
permanent search keys is worse than losing them.

## Testing

`src/test/.../MergeTeamsDEOTest.kt` and `TeamHistoryDEOTest.kt` provide the harness.

Backend:
- alias create / update / delete, including `ALIAS_ADDED` and `ALIAS_REMOVED` history
- collision rejection: against another alias, against any team's canonical name, against
  the alias's own team's canonical name
- normalization: `Zürich` and `Zurich` collide; case differences collide
- merge with aliases: kept, denied, edited; merge still succeeds when an alias is rejected
- rename with `keepOldNameAsAlias` set and unset; rename still succeeds when the alias is
  rejected
- alias re-pointing when an aliased team is itself merged away, including collision drops
- backfill over a multi-hop `mergedInto` chain
- `CacheUtil.deleteCachedTeamList()` is called on every alias mutation

Frontend:
- scoring and sort order across canonical / alias / amalgamation-member matches
- the alias-matched subtitle appears only when the match was alias-only
- alias chips render, add and remove in `TeamList.vue`
- merge and rename dialogs send the right `aliasesToCreate` / `keepOldNameAsAlias` payload
  for kept, denied and edited cases

## Out of scope

Locale-tagged or per-viewer name variants. Per-report stored spellings. Fuzzy or
edit-distance matching beyond the existing normalizer. Referee-submitted alias
suggestions or a review queue. Any change to the create-team flow. Rename backfill from
team history.
