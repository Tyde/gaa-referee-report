# Plan: MCP Admin Server + API Token Authentication

**Branch:** `feature/teams-mcp-server` (already checked out — commit tasks directly onto it)

**Architecture:** A revocable, DB-backed API token authenticates via a new Ktor bearer provider added
*alongside* existing session/JWT providers (nothing removed), and a new isolated `mcp-server/` Gradle
subproject (own Kotlin 2.x / Ktor 3.x client stack, official MCP Kotlin SDK, stdio transport) exposes
five focused admin tools that call the existing HTTP API with that token.

## Global Constraints

These bind every task and are copied verbatim into reviewer prompts:

- **Existing auth must not break.** Existing providers keep their names, order and challenge behavior.
  In every `authenticate(...)` list, session/JWT providers stay FIRST; `"auth-api-token"` is appended
  LAST. `ccc-session` routing is untouched.
- **Token management endpoints are admin-SESSION only** (mounted under `authenticate("admin-session")`
  alone). An API token must NOT be able to create/list/revoke API tokens.
- **Token format:** 32 bytes from `java.security.SecureRandom`, Base64-url without padding, prefixed
  `gge_`. Only the **SHA-256 hex** of the token is stored (64 chars). Plaintext is returned exactly
  once at creation. (SHA-256, not bcrypt: tokens are high-entropy.)
- **Validation rejects:** unknown token, `revoked = true`, `expiresAt` in the past, and any token whose
  linked user's role is not `UserRole.ADMIN` (re-checked on every request). On success `lastUsedAt`
  is updated.
- **No caching of API tokens** in CacheUtil — revocation must take effect immediately.
- **No new dependencies in the backend (root project).** Backend stays Ktor 2.3.13 / Kotlin 1.9.22.
  The bearer provider uses the existing `ktor-server-auth` dependency.
- **mcp-server module isolation:** its own `build.gradle.kts` with its own Kotlin 2.x + Ktor 3.x client
  + MCP SDK versions. Root `build.gradle.kts` dependencies and Kotlin version are NOT modified.
  Only `settings.gradle.kts` gains `include("mcp-server")`.
- **MCP tool surface (exactly five tools):** `list_teams`, `update_team`, `merge_teams`,
  `create_amalgamation`, `merge_tournaments`. Thin wrappers over existing HTTP endpoints.
- **Follow existing code patterns:** `lockedTransaction` for DB, `receiveAndHandleDEO` for endpoints,
  `ApiError`/`ApiErrorOptions` for errors, `@Serializable` DEOs, Exposed `LongIdTable` + `LongEntity`.
- **Do not stage or commit** pre-existing dirty/untracked files: `.gitignore`, `AGENTS.md`,
  `.claude/`, `.cursor/`, `.gemini/`. Stage only task-related files.
- Tests run against local Postgres (host/port from `GGERefereeConfig`, db `testing`, user `root`,
  password `testing`) via `TestHelper.setupDatabase()` / `tearDownDatabase()`.

## Reference: existing code facts (verified)

**Documented deviations discovered during implementation:**
1. `mcp-server` uses `includeBuild` (composite build) instead of `include` — Gradle does not allow two
   Kotlin Gradle plugin versions in one build; the composite build gives full toolchain isolation.
2. `mcp-server` uses minimal local `@Serializable` DEO models instead of the common submodule — the
   submodule DEO files import Exposed symbols, which would drag the Exposed stack into a client-only
   module. Local models are field-name-identical to the backend contract (verified in review).
3. The `auth-api-token` bearer provider has no `challenge` block — Ktor 2.3.13's bearer DSL does not
   expose `challenge` (verified via `javap` on the local jar). Invalid/expired/revoked tokens receive
   a bare `401` with `WWW-Authenticate` header and empty body instead of an `ApiError` JSON body.
4. Backend business errors are HTTP 200 + `ApiError` JSON (pre-existing `receiveAndHandleDEO`
   convention). The mcp-server detects `ApiError` bodies on 2xx and maps them to tool errors.

- Auth providers live in `src/main/kotlin/eu/gaelicgames/referee/plugins/Security.kt`
  (`install(Authentication) { jwt("auth-jwt"); form("auth-form"); session<UserSession>("auth-session");
  session<UserSession>("admin-session"); session<UserSession>("ccc-session") }`).
- Routing mounting in `src/main/kotlin/eu/gaelicgames/referee/plugins/Routing.kt`:
  `authenticate("auth-session", "auth-jwt") { refereeApiRouting() }`,
  `authenticate("admin-session") { adminApiRouting() }`, `authenticate("ccc-session") { CCCApiRouting() }`.
- `UserPrincipal(val user: User) : Principal` is in `src/main/kotlin/eu/gaelicgames/referee/data/User.kt`.
- Table registration: `DatabaseHandler.tables` list in
  `src/main/kotlin/eu/gaelicgames/referee/util/DatabaseUtil.kt`; `createSchema()` auto-creates new tables.
- Type-safe API resources: `src/main/kotlin/eu/gaelicgames/referee/resources/Api.kt`.
- Admin endpoints: `src/main/kotlin/eu/gaelicgames/referee/plugins/routing/AdminApiRouting.kt`.
- `/api/new_amalgamation` lives in `RefereeApiRouting.kt` (referee scope).
- `/api/teams_available` (GET, public) returns `List<TeamDEO>` incl. `amalgamationTeams`.
- DEO data classes (`TeamDEO`, `MergeTeamsDEO`, `NewAmalgamationDEO`, `MergeTournamentDEO`, `ApiError`,
  `ApiErrorOptions`) come from the git submodule `gaa-referee-report-common/src/main/kotlin`, wired into
  the backend via `java.sourceSets["main"].java.srcDir(...)` in the root `build.gradle.kts`.
- Existing endpoints used by MCP tools:
  - `POST /api/team/update` body `TeamDEO{name,id,isAmalgamation,amalgamationTeams?}` → `TeamDEO`
  - `POST /api/team/merge` body `MergeTeamsDEO{baseTeam, teamsToMerge[]}` → `TeamDEO`
  - `POST /api/new_amalgamation` body `NewAmalgamationDEO{name, teams[]}` → `TeamDEO`
  - `POST /api/tournament/merge` body `MergeTournamentDEO{mergeFromId, mergeToId}` → `TournamentDEO`
  - API errors are JSON `ApiError{error: String, message: String}`.

## Task 1: Backend — API token storage and validation

**Files to create:**
- `src/main/kotlin/eu/gaelicgames/referee/data/ApiToken.kt`
- `src/main/kotlin/eu/gaelicgames/referee/data/api/ApiTokenDEO.kt`
- `src/test/kotlin/eu/gaelicgames/referee/data/api/ApiTokenDEOTest.kt`

**File to modify:** `src/main/kotlin/eu/gaelicgames/referee/util/DatabaseUtil.kt`
(add `ApiTokens` to the `DatabaseHandler.tables` list — placed after `Users`/`Sessions` family; nothing else).

**`data/ApiToken.kt`:**

```kotlin
object ApiTokens : LongIdTable() {
    val name = varchar("name", 100)
    val tokenHash = varchar("token_hash", 64).index()
    val user = reference("user", Users)
    val createdAt = datetime("created_at")
    val expiresAt = datetime("expires_at").nullable()
    val revoked = bool("revoked").default(false)
    val lastUsedAt = datetime("last_used_at").nullable()
}

class ApiToken(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ApiToken>(ApiTokens)
    var name by ApiTokens.name
    var tokenHash by ApiTokens.tokenHash
    var user by User referencedOn ApiTokens.user
    var createdAt by ApiTokens.createdAt
    var expiresAt by ApiTokens.expiresAt
    var revoked by ApiTokens.revoked
    var lastUsedAt by ApiTokens.lastUsedAt
}
```

**`data/api/ApiTokenDEO.kt`** — `@Serializable` DEOs + logic (datetimes serialized as ISO-8601
`String` via `LocalDateTime.toString()` / `LocalDateTime.parse()` to keep serialization trivial):

- `NewApiTokenDEO(name: String, expiresInDays: Int? = null)`
- `ApiTokenCreatedDEO(id: Long, name: String, token: String, expiresAt: String?)`
- `ApiTokenDEO(id: Long, name: String, createdAt: String, expiresAt: String?, revoked: Boolean, lastUsedAt: String?)`
  with `fromApiToken(token: ApiToken)` companion mapper (never includes the hash)
- `RevokeApiTokenDEO(id: Long)`
- `suspend fun NewApiTokenDEO.createInDatabase(admin: User): Result<ApiTokenCreatedDEO>` —
  generates the token (see Global Constraints for format), stores hash + metadata in a
  `lockedTransaction`, returns the created DEO with plaintext token.
- `suspend fun RevokeApiTokenDEO.revoke(): Result<ApiTokenDEO>` — sets `revoked = true`; failure if
  token id not found.
- `suspend fun apiTokenList(): List<ApiTokenDEO>` — all tokens via `fromApiToken`.
- `suspend fun validateApiToken(rawToken: String): Result<User>` — SHA-256 hex of `rawToken`, indexed
  lookup, apply every rejection rule from Global Constraints, update `lastUsedAt` on success, return
  the linked `User`.

**Tests (`ApiTokenDEOTest.kt`)** — follow `MergeTeamsDEOTest` structure (JUnit 5, backticked names,
`TestHelper.setupDatabase()` in `@BeforeEach`, `TestHelper.tearDownDatabase()` in `@AfterEach`).
Use TDD: write tests first, watch them fail, then implement.
Cover: create returns plaintext with `gge_` prefix and stores only the hash; validate succeeds for a
fresh token (and bumps `lastUsedAt`); unknown token rejected; revoked token rejected; expired token
rejected; token of a non-admin user rejected; revoke flow.

**Verify:** `./gradlew test --tests "eu.gaelicgames.referee.data.api.ApiTokenDEOTest"` then the full
`./gradlew test` before committing.

## Task 2: Backend — bearer auth provider, routing wiring, token management endpoints

Depends on Task 1 (uses `validateApiToken`, DEOs).

**Files to modify:**
- `src/main/kotlin/eu/gaelicgames/referee/plugins/Security.kt`
- `src/main/kotlin/eu/gaelicgames/referee/plugins/Routing.kt`
- `src/main/kotlin/eu/gaelicgames/referee/resources/Api.kt`

**Files to create:**
- `src/main/kotlin/eu/gaelicgames/referee/plugins/routing/AdminTokenManagementRouting.kt`
- `src/test/kotlin/eu/gaelicgames/referee/plugins/ApiTokenAuthTest.kt`

**Security.kt:** extract the new provider into an internal reusable config function so tests can
install the identical configuration:

```kotlin
internal fun AuthenticationConfig.configureApiTokenAuth() {
    bearer("auth-api-token") {
        realm = "Admin API token"
        authenticate { credential ->
            validateApiToken(credential.token).map { UserPrincipal(it) }.getOrNull()
        }
        challenge {
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError(ApiErrorOptions.NOT_AUTHORIZED, "API token is not valid, expired or revoked")
            )
        }
    }
}
```

(Exact lambda shapes may need adjustment to Ktor 2.3.13's bearer DSL — behavior must match the spec.)
Call `configureApiTokenAuth()` inside the existing `install(Authentication) { ... }` in
`configureSecurity()`. Do not modify the existing providers.

**Routing.kt:** change mounting to:

```kotlin
authenticate("auth-session", "auth-jwt", "auth-api-token") { refereeApiRouting() }
authenticate("admin-session", "auth-api-token") { adminApiRouting() }
authenticate("admin-session") { adminTokenManagementRouting() }
authenticate("ccc-session") { CCCApiRouting() }
```

**Api.kt:** add (note: class name `ApiToken` also exists in `data` package — mind imports):

```kotlin
@Serializable
@Resource("api_token")
class ApiToken(val parent: Api) {
    @Serializable
    @Resource("new")
    class New(val parent: ApiToken)

    @Serializable
    @Resource("all")
    class All(val parent: ApiToken)

    @Serializable
    @Resource("revoke")
    class Revoke(val parent: ApiToken)
}
```

**AdminTokenManagementRouting.kt** — `fun Route.adminTokenManagementRouting()`:
- `post<Api.ApiToken.New>`: receive `NewApiTokenDEO`; get calling admin via
  `call.principal<UserPrincipal>()`; call `createInDatabase(user)`; respond with
  `ApiTokenCreatedDEO` (plaintext, one time).
- `get<Api.ApiToken.All>`: respond `apiTokenList()`.
- `post<Api.ApiToken.Revoke>`: `receiveAndHandleDEO<RevokeApiTokenDEO> { it.revoke() ... }`.

**ApiTokenAuthTest.kt** — focused HTTP test using Ktor `testApplication` with a MINIMAL module
(not the full app): `TestHelper.setupDatabase()` first (validation hits the DB), then install
`Authentication` via the same `configureApiTokenAuth()`, and a route
`authenticate("auth-api-token") { get("/protected") { call.respond(HttpStatusCode.OK) } }`.
Assert: no header → 401; garbage token → 401; valid token (created via
`NewApiTokenDEO.createInDatabase` for an admin user) → 200; revoked token → 401.
`tearDownDatabase()` after. Then run full `./gradlew test`.

## Task 3: Frontend — admin token management UI

Depends on Tasks 1-2 for the API contract (reproduced below — this is the whole contract):

- `POST /api/api_token/new` — body `{name: string, expiresInDays?: number|null}` →
  `200 {id: number, name: string, token: string, expiresAt: string|null}` (token shown once)
- `GET /api/api_token/all` → `200 [{id, name, createdAt: string, expiresAt: string|null, revoked: boolean, lastUsedAt: string|null}]`
- `POST /api/api_token/revoke` — body `{id: number}` → `200 {id, ...ApiTokenDEO}` or `ApiError{error, message}`

**Work (frontend-vite/, Composition API + pinia + primevue, follow existing admin components):**
- Add zod schemas/types in the appropriate `src/types/` file (follow `referee_types.ts` patterns).
- Add `createApiToken`, `listApiTokens`, `revokeApiToken` to `src/utils/api/admin_api.ts`
  (`makePostRequest`/`fetch` + `parseAndHandleDEO` like the existing functions).
- New `src/components/admin/ApiTokenManager.vue`: table of tokens (name, created, expiry, last used,
  status), create form (name + optional expiry-in-days), one-time plaintext token display with a
  copy button immediately after creation, revoke action with confirmation. Match look/feel of the
  existing admin editors (read `src/components/admin/` siblings, e.g. the user and teams editors,
  and check whether they use i18n — if they do, follow that pattern).
- Wire the component into `AdminApp.vue` (or wherever admin sections are registered/navigation lives).
- **Verify:** `cd frontend-vite && npm run lint && npm run build` must pass. Do not commit
  `src/main/resources/static` build artifacts if the build outputs there (check what `npm run build`
  writes and whether it is gitignored — leave untracked build output uncommitted).

## Task 4: mcp-server Gradle subproject (stdio MCP server)

Independent of Tasks 1-3 code-wise; uses the same HTTP contract as Task 3 plus the admin endpoints
listed in "Reference: existing code facts".

**Files to create/modify:**
- Modify `settings.gradle.kts`: add `include("mcp-server")` (keep `rootProject.name` line).
- `mcp-server/build.gradle.kts`: own plugins — `kotlin("jvm") version "2.4.0"`,
  `kotlin("plugin.serialization") version "2.4.0"`, `com.github.johnrengelman.shadow` (same version as
  root, 8.1.1), `application`. Deps: `io.modelcontextprotocol:kotlin-sdk:0.15.0`,
  `io.ktor:ktor-client-cio:3.5.1`, `io.ktor:ktor-client-content-negotiation:3.5.1`,
  `io.ktor:ktor-serialization-kotlinx-json:3.5.1`, kotlinx-serialization-json, a logger (slf4j-simple
  is fine), `kotlin("test")` for tests. Reuse shared DEOs via
  `java.sourceSets["main"].java.srcDir("${rootDir}/gaa-referee-report-common/src/main/kotlin")`
  (same trick as root). If the submodule DEOs drag in unwanted dependencies, fall back to minimal
  local `@Serializable` request/response models and note it in the report.
  Set `mainClass` and configure the shadow jar as `mcp-server-all.jar`.
- `mcp-server/src/main/kotlin/eu/gaelicgames/referee/mcp/Main.kt`
- `mcp-server/src/test/kotlin/eu/gaelicgames/referee/mcp/` — tool-handler tests.

**Main.kt design:**
- Config from env vars: `GGE_SERVER_URL` (base URL, e.g. `https://referee.example.com`) and
  `GGE_API_TOKEN` (the `gge_...` token). Fail fast with a clear message on stderr if missing.
- Ktor CIO client: `Authorization: Bearer <token>`, ContentNegotiation JSON, base URL prepended.
- Tool handlers as plain testable suspend functions (one per tool) taking the client + typed args and
  returning a result string (pretty JSON) or throwing/mapping `ApiError` — then wire them into MCP
  `Server` tool definitions with JSON input schemas, served over `StdioServerTransport`.
- Tools (exactly these five):
  - `list_teams` (no args) → `GET /api/teams_available`
  - `update_team` `{id, name, isAmalgamation, amalgamationTeams?}` → `POST /api/team/update`
    (this is how amalgamation membership is edited)
  - `merge_teams` `{baseTeam, teamsToMerge[]}` → `POST /api/team/merge`
  - `create_amalgamation` `{name, teams[]}` → `POST /api/new_amalgamation`
  - `merge_tournaments` `{mergeFromId, mergeToId}` → `POST /api/tournament/merge`
- API error responses (`ApiError` JSON) must surface as MCP tool errors (`isError = true` with the
  message), not crashes. Logging goes to stderr only (never stdout — stdout is the MCP channel).

**Tests:** use Ktor client `MockEngine` to verify each tool handler hits the right path with the
right method/body and maps both success and `ApiError` responses correctly. No real MCP transport
needed in tests.

**Verify:** `./gradlew :mcp-server:build`, `./gradlew :mcp-server:test`,
`./gradlew :mcp-server:shadowJar`, and root `./gradlew build` + `./gradlew test` must all still pass.
