# Repository Guidelines

## Project Structure & Module Organization
- Backend (Kotlin/Ktor): `src/main/kotlin/eu/gaelicgames/referee/...`
- Common API Data Exchange Object Classes: `gaa-referee-report-common/src/main/kotlin/...`
- Backend resources/templates: `src/main/resources`
- Tests (JUnit 5): `src/test/kotlin/...`
- Frontend (Vue 3 + Vite): `frontend-vite/` (build outputs are served by the backend)
- Vue 3: Composition API style, pinia stores, and primevue ui elements
- Docker/Dev: `docker-compose.yml`, `docker-compose.dev.yml`
- Gradle build: `build.gradle.kts`, `settings.gradle.kts`
- Database is handled with the Kotlin Expose library and is found in the ReportData.kt file.

## Build, Test, and Development Commands
- Java runtime: set `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home` before running Gradle commands
- Backend build + tests: `./gradlew build`
- Run backend locally: `./gradlew run` (starts Ktor on `:8080`)
- Unit tests only: `./gradlew test`
- Uber JAR (optional): `./gradlew shadowJar`
- Frontend one-off build: `cd frontend-vite && npm install && npm run build`
- Frontend watch build: `cd frontend-vite && npm run watch-build`
- Full dev stack via Docker: `docker compose -f docker-compose.dev.yml up --build`

## Coding Style & Naming Conventions
- Kotlin: use official style (`kotlin.code.style=official`, 4-space indent). Package root: `eu.gaelicgames.referee`.
- One top-level type per file; descriptive names (e.g., `SanitizeDataService.kt`).
- Tests: mirror package, name classes `*Test.kt` and methods with backticked, descriptive names.
- Frontend: TypeScript + Vue 3. Use ESLint and `npm run lint` before committing.


## Testing Guidelines
- Framework: JUnit 5 with Ktor test utilities; see examples under `src/test/kotlin`.
- Run locally: `./gradlew test` (uses JUnit Platform).
- Add tests alongside changes; prefer small, deterministic tests. Keep DB helpers under `TestHelper` patterns.

## Commit & Pull Request Guidelines
- Commits: imperative mood and concise titles (e.g., "Fix race in SanitizeDataService"). Reference issues (`#123`) when relevant.
- PRs: clear description, linked issues, reproduction steps, and risk/rollout notes. Include screenshots/GIFs for UI changes (frontend).
- CI should be green: backend tests pass and frontend lints/builds.

## Security & Configuration Tips
- Configure via env vars or `gge-referee.properties` (see `gge-referee.properties.sample`). Common vars: `SERVER_URL`, `MAILJET_PUBLIC/SECRET`, `REDIS_*`, `POSTGRES_*`.
- Do not commit secrets. For local dev, prefer `docker-compose.dev.yml` which provisions Postgres and Redis.
- Optional seeding: set `ADD_MOCK_DATA=true` to generate sample data at startup.
