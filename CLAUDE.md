# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

RedCheck API is the Spring Boot backend for RedCheck, a task-prioritization app. It exposes a REST API consumed by a separate React frontend (`redcheck-frontend`), and offloads AI-based task prioritization to an external "SmartCheck AI" engine (a separate Python/FastAPI service, RAG-backed by Gemini) reached over HTTP. The frontend and the SmartCheck AI engine live in sibling repos and are sometimes relevant here — e.g. changing the `EngineRequestDTO` payload shape or the `/api/v1/prioritize` contract requires a coordinated change on the SmartCheck AI side, and DTO/response shape changes can affect the frontend. Flag this kind of cross-repo impact when it comes up.

## Commands

Use the Maven wrapper (`./mvnw`), not a system-installed Maven.

```bash
./mvnw spring-boot:run          # run the app locally (needs env vars, see below)
./mvnw clean package            # compile, run tests, and package the jar
./mvnw test                     # run the full test suite
./mvnw test -Dtest=TaskServiceTest                       # run a single test class
./mvnw test -Dtest=TaskServiceTest#shouldCreateTask       # run a single test method
```

JaCoCo is wired into the `test` phase (`mvn test`), producing a coverage report under `target/site/jacoco/`.

There is no linter/formatter configured in this repo.

### Running locally

The app reads all config through env vars referenced in `src/main/resources/application.yaml` — there is no committed `application-local.yml`/`.properties` (it's gitignored). At minimum you need: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `AI_ENGINE_URL`. Optional: `DDL_AUTO`, `SHOW_SQL`, `CORS_ALLOWED_ORIGINS`. Swagger UI / OpenAPI docs are disabled by default (`springdoc.api-docs.enabled=false`) — flip via env/profile if needed.

Docker: multi-stage `Dockerfile` builds with Maven then runs on a non-root user in `eclipse-temurin:17-jre-alpine`, exposing 8080. CI (`.github/workflows/deploy.yml`) builds/tests on push to `main`, pushes the image to Docker Hub, and deploys over SSH via `docker compose`.

## Architecture

Standard layered structure under `com.redcheck.backend`: `controller` → `service` → `repository` (Spring Data JPA) → `entity`, with `dto/{request,response,update}` used to keep entities off the wire. Controllers never receive/return entities directly.

### Domain model

`User` owns four collections cascaded with `orphanRemoval`: `Subject`, `Task`, `RecurringTask`, `Notification`, `AiResponse`, `ProgressRecord` — data is always scoped per-user. `Subject` → `Task` is also a cascading one-to-many. Ownership is enforced in the service layer (see `*NotOwnedException` classes), not by query filtering alone — when adding endpoints, verify the resource's owner matches `currentUser` before acting on it.

Soft-delete ("Trash") is a real field (`deletedFalse`/`deleted` filters appear throughout repository queries), not JPA `@Where`/`@SQLDelete`. Endpoints have `DELETE /{id}` (soft) and `DELETE /{id}/force` (hard) variants, plus `PATCH /{id}/restore`. `TrashCleanupSchedulerService` purges soft-deleted records on a schedule.

**Custom exceptions** (`exception` package: `TaskNotFoundException`, `TaskNotOwnedException`, `SubjectAlreadyExistsException`, etc.) are plain `RuntimeException` subclasses with no `@ResponseStatus` and there is no `@ControllerAdvice`/global exception handler anywhere in the app — they are not currently mapped to HTTP status codes at a central point. Keep this in mind when adding new failure cases; check how existing controllers/tests expect these to surface before assuming Spring maps them for you.

**Dashboard aggregate endpoint (`GET /subjects/with-tasks`):** fixes an N+1 the frontend used to have — it used to call `GET /subjects` then one `GET /subjects/{id}/tasks?completed=false` per subject. `SubjectService#getAllSubjectsWithTasks` instead does exactly two queries total (`SubjectRepository#findAllByUserAndDeletedFalse` + the new `TaskService#getPendingOrTodayTasks`, which reuses the existing `findPendingOrCompletedTodayAndDeletedFalse` query without the in-memory `subjectId` filter `TaskService#getAllTask` applies) and groups tasks by `subjectId` in memory (`Collectors.groupingBy`), returning `List<SubjectWithTasksResponseDTO>` (a `SubjectResponseDTO` shape plus a nested `tasks` list). If the dashboard ever needs a different task subset (e.g. all tasks, not just pending/completed-today), extend this pair rather than reintroducing a per-subject loop on the frontend.

**Calendar history endpoint (`GET /tasks?from=YYYY-MM-DD&to=YYYY-MM-DD`):** a top-level endpoint (`TaskCalendarController`, deliberately *not* nested under `/subjects/{id}`, same N+1-avoidance reasoning as the dashboard endpoint above) that returns every task — pending **or completed** — across all of the user's subjects whose `deadline` falls within the given range, via `TaskService#getTasksForDateRange` and the new repository method `findAllBySubject_User_IdAndDeadlineBetweenAndDeletedFalse`. This exists because `findPendingOrCompletedTodayAndDeletedFalse` (used by the dashboard) only ever covers *today's* pending/completed tasks — it has nothing for a task that was completed on some earlier day, which is exactly what the frontend calendar needs for past-day history. `from`/`to` are both required and inclusive; the frontend fetches one range per visible calendar view (a single day, a week, or a whole month) rather than one call per day.

### Authentication

Stateless JWT (`io.jsonwebtoken`), configured in `security/`: `JwtService` (issue/parse/validate tokens), `JwtAuthenticationFilter` (a `OncePerRequestFilter` that reads the `Authorization: Bearer` header, skips `/auth/**`, and populates `SecurityContextHolder`), `SecurityConfig` (stateless session policy, permits `/auth/**` and Swagger paths, requires auth on everything else — including `/users/**` explicitly). `@AuthenticationPrincipal User currentUser` is the standard way controllers get the caller's identity. CORS origins come from `app.cors.allowed-origins` and are also referenced per-controller via `@CrossOrigin(origins = "${app.cors.allowed-origins}")`.

### Recurring tasks & schedulers

`RecurringTask` supports simple periodicities (`DAILY`, `WEEKLY`, `BIWEEKLY`, `MONTHLY`, validated/advanced via `util/FrequencyUtils`) as well as non-simple ones handled elsewhere in `RecurringTaskService`. `RecurringTaskSchedulerService` generates the next `Task` occurrences; `ProgressRecordSchedulerService` computes recurring progress snapshots. Both run via Spring's `@Scheduled` (`@EnableScheduling` is on in `RedCheckApiApplication`).

### SmartCheck AI integration

`SmartCheckAIService.runDailySmartAnalysis` is `@Async` (`@EnableAsync` is on): it gathers the user's pending tasks plus per-subject completion-ratio analytics from `TaskRepository`, posts them as `EngineRequestDTO` to `${ai.engine.url}/api/v1/prioritize` via `RestTemplate`, and persists the raw JSON response as an `AiResponse` (type `DAILY_ANALYSIS`), creating a `Notification` on success or failure. Results are fetched later via `getTodaysAnalysis` (only returns a result if it was created today) — the AI call itself is fire-and-forget from the controller's perspective. `lang` is passed through so the external engine can localize output.

### Tests

Tests live flat under `src/test/java/com/redcheck/backend` (no sub-packages), pairing `*ServiceTest` (Mockito-based unit tests) with `*ControllerIntegrationTest` (`@WebMvcTest` + `MockitoBean` for services/`JwtService`, `MockMvc` for HTTP-level assertions, Jackson via `tools.jackson.databind.ObjectMapper`). There's no `@SpringBootTest`-with-real-DB integration layer — controller tests mock the service layer, so they don't exercise JPA/DB behavior. This is the intended style: keep writing new tests as mocked-service unit/`@WebMvcTest` pairs rather than introducing a real-DB integration layer (e.g. Testcontainers).
