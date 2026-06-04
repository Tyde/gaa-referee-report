# Repository Guidelines

## Project Structure
- Backend: Ktor on Netty, port 8080. Entry: `Application.kt:20` → `ApplicationKt` (set in `build.gradle.kts:20`)
- Frontend: Vue 3 + Vite in `frontend-vite/`. Builds to `src/main/resources/static` (set in `vite.config.ts:64`). Backend serves these.
- `gaa-referee-report-common/` is a git submodule — run `git submodule update --init` before building
- Shared DEO classes live in the submodule under `*/Base.kt` and are copied into backend source set (`build.gradle.kts:31-33`)
- `referee-kottster/` is a separate Kottster admin panel, not wired into the main build

## Build & Dev Commands
- Backend: `./gradlew build`, `./gradlew run`, `./gradlew test`
- Single test class: `./gradlew test --tests "eu.gaelicgames.referee.data.api.GameReportDEOTest"`
- Frontend build: `cd frontend-vite && npm install && npm run build` (outputs to backend static dir)
- Frontend watch: `cd frontend-vite && npm run watch-build`
- Docker dev stack: `docker compose -f docker-compose.dev.yml up --build`
- Fat JAR: `./gradlew shadowJar`

## Configuration
- Local config: `gge-referee.properties` (gitignored; see `gge-referee.properties.sample`)
- Env vars override properties. Key vars: `SERVER_URL`, `MAILJET_PUBLIC/SECRET`, `REDIS_*`, `POSTGRES_*`, `OBJECTSTORAGE_*`
- `ADD_MOCK_DATA=true` seeds 280 mock reports + users/tournaments/teams at startup
- `CLAUDE_ACCESSTOKEN` enables AI-powered rule translation (`RuleTranslationUtil.kt`)
- Kotlin version in `build.gradle.kts:11` (1.9.22) overrides `gradle.properties:2` (1.8.22)

## Testing
- Framework: JUnit 5 + Ktor test utilities
- Tests need `TestHelper.setupDatabase()` / `tearDownDatabase()` — they handle schema creation and cleanup
- `USE_POSTGRES` flag in tests; CI runs against a Postgres service container (see `.github/workflows/gradle.yml:23-40`)
- Cypress is configured (`frontend-vite/cypress.config.ts`) but has no CI workflow

## Architecture Notes
- Plugin init order matters: Templating → Serialization → Security → Routing (`Application.kt:99-102`)
- Three background coroutines launch on startup: `NotifyCCCService`, `CleanExpiredDataService`, `SanitizeDataService`
- Auth: session + JWT (RSA-256). Three scopes: `referee`, `admin`, `ccc` (see `plugins/Routing.kt:83-93`)
- Type-safe routing via Ktor Resources (`resources/Api.kt`)
- DB schema: `ReportData.kt` (Exposed ORM, all tables in one file)
- Redis cache: `CacheUtil.kt`, configured in `Application.kt:33`

## Style Conventions
- Kotlin: official style, 4-space indent (`kotlin.code.style=official` in `gradle.properties`)
- One top-level type per file; `*Test.kt` for tests with backticked method names
- Frontend: Composition API, pinia stores, primevue UI. Run `npm run lint` before committing
