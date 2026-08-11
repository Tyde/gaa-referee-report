# Team Name Aliases — Progress Report

**Date:** 2026-08-11
**Branch:** `feature/team_spelling_support`
**Plan:** `docs/superpowers/plans/2026-08-11-team-name-aliases.md`
**Spec:** `docs/superpowers/specs/2026-08-11-team-name-aliases-design.md`
**Status:** Backend tasks 1–7 complete and reviewed. Stopped here by request. Tasks 8–14 not started.

## What works now

The backend half of the feature is in place. A team can carry any number of
admin-maintained alternative spellings; the canonical name is still the only thing
displayed anywhere. Merging and renaming both offer to keep the disappearing name
as a spelling, and the API serves the spellings alongside every team so the
frontend can search them without a new endpoint.

Nothing is wired into the UI yet — that is tasks 9–13.

## Commits on this branch

Ten commits, oldest first, on top of `1abb653`:

| Commit | Task | What |
|---|---|---|
| `3448aa4` | 1 | Shared team name normalizer |
| `3ff4e42` | 2 | `TeamAliases` table, `TeamAlias` entity, two history change types |
| `f6008ea` | 3 | Alias create/update/delete with collision validation |
| `a653604` | 4 | Aliases in the team list and single-team responses |
| `386358d` | 4 fix | Run `fromTeam` inside a transaction at four routing call sites |
| `28ff9a2` | 5 | Admin routes for alias management |
| `c3f00d7` | 6 | Merge keeps the merged team's name and aliases |
| `d67e7e3` | 6 fix | Carried-alias collision tests |
| `8fd8506` | 7 | Rename can keep the old name as a spelling |
| `1bae5dd` | 7 fix | Unchanged-name gate tests |

## Submodule state — read this before continuing

`gaa-referee-report-common/` is a git submodule pointing at
`github.com/Tyde/gaa-referee-report-common.git`, and the alias DEOs live in it.

- All submodule work is on branch **`feature/team-aliases`**, currently at `5a6b5aa`.
- The submodule's `main` is at `d0fc268` and must stay there until this feature merges.
- During Task 3 an implementer pushed to the submodule's `main` unasked. That was undone:
  the commit was moved onto `feature/team-aliases` and `main` was force-reset back to
  `d0fc268`. Every implementer since has been told to push only the feature branch.

Submodule commits so far: `8d5b8fe` (alias DEOs), `2b738b3` (`TeamDEO.aliases`),
`8df2837` (`MergeTeamsDEO.aliasesToCreate`), `5a6b5aa` (`UpdateTeamDEO`).

**Before merging this feature**, the submodule branch needs to land on its `main` and the
gitlink needs to point at the merged commit — otherwise anyone cloning the parent repo gets
a gitlink that only resolves via a feature branch.

## What remains

| Task | Scope |
|---|---|
| 8 | Backfill aliases from already-merged teams (Migration 10 backfill half) |
| 9 | Vitest setup, TypeScript normalizer, alias-aware search scoring |
| 10 | Team picker uses alias search, matched-alias subtitle, `:key` fix, i18n |
| 11 | Alias API client and admin chip editor |
| 12 | Merge dialog per-team alias rows |
| 13 | Rename control in three admin components |
| 14 | Full verification, including the end-to-end walkthrough |

Task 8 is the last backend task and is independent of the frontend work.
Tasks 10–13 all depend on task 9 and are independent of each other once it lands.

## Corrections to the plan — do not "fix" the code to match the prose

Two statements in the plan turned out to be wrong. Both were confirmed by
independent investigation, and in both cases the **code is correct and the prose is not**.

1. **The merge alias ordering is not load-bearing.** The plan claims the requested alias
   must be created after re-pointing carried aliases so "an identical carried alias wins
   the unique index". The two operations actually commute: `validateAliasInTransaction`
   is ownership-agnostic, so in the swapped order the create fails against the merged
   team's own still-present row, and re-pointing mutates a row in place rather than
   delete-and-recreate. Both orderings leave the same original row on the survivor, with
   the same id and the same history events. The outcome the plan describes is right; the
   mechanism it gives is not.

2. **"Two merged teams both carrying the same alias spelling" is unconstructible.** The
   global unique index on `TeamAliases.normalized` forbids that state, so the test the
   plan asks for cannot be written. The equivalent that *can* exist — a carried alias
   colliding with another merging team's live canonical name — is covered instead.

A third plan gap: Task 7's brief listed four call sites to migrate from `TeamDEO` to
`UpdateTeamDEO`; there were five. The fifth was in `TeamAliasDEOTest.kt`.

## Deferred findings for the final review

None of these block the remaining tasks. They are recorded here so the final
whole-branch review can triage them rather than rediscovering them.

1. **Alias rename is recorded as `ALIAS_ADDED`.** `UpdateTeamAliasDEO.updateInDatabase`
   writes an `ALIAS_ADDED` history event when an alias is edited, so the team history UI
   labels a rename as "Alternative spelling added". There is no `ALIAS_UPDATED` enum value.

2. **No route-level tests.** The regression tests added in Task 4 reimplement the fixed
   call shape inline rather than invoking the real Ktor handlers, so stripping
   `lockedTransaction` from `RefereeApiRouting.kt:141`/`:150` or `AdminApiRouting.kt:149`/`:160`
   would not be caught. Closing this means introducing `testApplication` infrastructure the
   codebase does not have. **Ruled on 2026-08-11: the plan governs, deferred.** The same
   ruling covers Task 5's alias routes shipping without automated tests.

3. **Whitespace- and case-only renames count as renames.** The gate around
   `keepOldNameAsAlias` is a raw `!=` comparison, so `"FC Zurich"` → `"FC Zurich "` fires a
   `RENAMED` history event and attempts an alias. Self-limiting — the alias normalizes to
   the team's own new name and is rejected — so the only symptom is history-log noise.
   Fixing it means normalizing the gate comparison, which is a production change.

4. **Cache invalidation is untested.** No test asserts `CacheUtil.deleteCachedTeamList()`
   is called on alias mutations; it was verified by source reading. Redis is not connected
   in the test environment, which matches existing practice in this codebase.

5. **Task 5's manual route check was never performed.** No server could be started in the
   implementer's session, so `/api/team/alias/new` and its siblings have not been exercised
   over HTTP at all. This must happen in Task 14's walkthrough.

## How to resume

The SDD ledger at `.superpowers/sdd/2026-08-11-team-name-aliases/progress.md` holds the
full task-by-task record, including every fix round and ruling. It is git-ignored scratch —
this document is the durable summary.

To continue, resume subagent-driven execution of the plan at Task 8. Each remaining task's
brief can be regenerated with the `task-brief` script against the plan file.
