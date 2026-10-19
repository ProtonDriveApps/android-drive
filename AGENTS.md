# AGENTS.md

Guidance for agents working in this repository. This file is always loaded; the detailed
documentation lives under [docs/agents/](docs/agents/README.md) and is read on demand.
`FLANDERS.md` defines the MR review bot's mandate (it reviews non-draft MRs against this file).

## Before planning a change

Any change that will end in an MR starts by reading [docs/agents/README.md](docs/agents/README.md)
and the documents it lists for the kind of change you are making. Plans must fit the architecture
described there.

## Build & test commands

Android app, Gradle + Kotlin DSL, ~200 modules. `dev` is the default product flavor.

```bash
./gradlew assembleDevDebug                 # default dev build (points at proton.black by default)
./gradlew assembleProdDebug                # production build; apk in app/build/outputs/apk/prod/debug/
./gradlew :app:getArchivesVersion          # prints version name

# Unit tests. Library modules have no flavors, :app does.
./gradlew testDebugUnitTest testDevDebugUnitTest -x :screenshot-tests:presentation:testDebugUnitTest
./gradlew :drive:backup:data:testDebugUnitTest
./gradlew :drive:link:data:testDebugUnitTest --tests "*LinkRepositoryImplTest"
./gradlew :drive:link:data:testDebugUnitTest --tests "*LinkRepositoryImplTest.empty"

./gradlew multiModuleDetekt --configuration-cache-problems=warn   # static analysis (all modules)
./gradlew :app:lintDevDebug                                        # android lint (abortOnError = true)

./gradlew screenshot-tests:presentation:verifyPaparazziDebug       # screenshot tests (CI only)
./gradlew screenshot-tests:presentation:recordPaparazziDebug       # re-record snapshots (CI only)

./gradlew :app:connectedDevDebugAndroidTest                        # instrumented tests on a device
./gradlew :app:pixel2api30DevDebugAndroidTest                      # ...on the declared managed device
```

Local config (both git-ignored): `local.properties` needs `sdk.dir`. Optional `private.properties`
at the repo root supplies `HOST` (dev backend, defaults to `proton.black`) and `PROXY_TOKEN`; the
build warns and continues without it. `TEST_ENV_DOMAIN` overrides `HOST` in CI.

Proton Core modules are pulled in by the `include-core-build` settings plugin, not vendored here.
The Drive SDK (`me.proton.drive:sdk`) is an external artifact; set `me.proton.drive.sdk.path` in
`gradle.properties` to substitute a local composite build. `dev`/`alpha` resolve the `-rust` crypto
SDK variant, `beta`/`prod` the Go one — this is forced per-variant in `app/build.gradle.kts`.

## Conventions

- New Kotlin files carry the GPLv3 Proton header; copy it from a neighbouring file, but set the
  year to the current year (`Copyright (c) <current year> Proton AG.`). Do not change the year on
  existing files.
- Commits on feature branches and MR titles use a plain imperative subject without a
  conventional-commit prefix; commits add an explanatory body. MRs are squashed on merge, and the
  merge commit onto `develop` adds the conventional-commit type (`feat:`, `fix:`, `chore:`,
  `refactor:`, `test:`), which release candidates are built from.
- Detekt runs with a `maxIssues` budget and is `allow_failure` in CI, but do not add new
  violations. Its config is generated into the git-ignored `config/detekt/` by the Proton Core
  detekt plugin; do not edit it.

## Team Conventions

- Avoid deprecated APIs; use the recommended replacement wherever possible.
- Never swallow `CancellationException`: log or post what you need, then rethrow it. A
  `TimeoutCancellationException` from your own `withTimeout` is not a cancellation of the caller;
  turn it into a regular error (or use `withTimeoutOrNull`) instead of letting it escape.
- Code that cancels work owns the cleanup that goes with it. A worker is not told reliably that it
  was cancelled for good (queued work never runs, and stop reasons are only known on API 31+), so
  it must not be the only place that cleans up.
- Pick `ExistingWorkPolicy` deliberately. `KEEP` is the default for unique per-user work; do not use
  `APPEND_OR_REPLACE` or `REPLACE` without a stated reason in the commit or MR.
- Chunk `IN (:list)` queries to the SQLite variable limit (999 on older Android) unless the list is
  bounded by `dbPageSize`.
- Prefer one bulk statement over per-row loops for deletes and updates; if a per-row guard is
  needed, find a single-statement formulation first.
- The ViewModel owns UI state. Do not shadow it with `rememberSaveable` in the composable.
- Tables that reference a link, share or account carry a `ForeignKey` with the right
  `onDelete` (usually `CASCADE`), modelled on `LinkOfflineEntity`.
- Never hardcode one `LinkId` subtype for another (`FileId` for a folder). Derive the concrete type
  from the link properties tables, or keep the `LinkId` you were given.
- Minimal diff first: among the solutions that follow the architecture in
  [docs/agents/](docs/agents/README.md) (Clean Architecture, SOLID, reusable solutions), prefer the
  one with the fewest changes. A smaller diff never justifies breaking those rules, such as
  skipping a layer or special-casing a feature where a shared component lacks a general capability.
  Do not change DAOs, schemas or abstractions the solution does not need, and do not bundle
  unrelated refactors. No arbitrary caps or batch limits.

## Skills

Skills live in the committed **`.claude/skills/<name>/SKILL.md`** directory —
a plain, transparent location everyone can see and review. Agents that do not load skills
automatically should read the relevant `SKILL.md` as a regular document. Add a new shared
skill by creating `.claude/skills/<name>/SKILL.md` (plus any bundled scripts)
and committing it.

### Available skills

- **[safe-logging](.claude/skills/safe-logging/SKILL.md)** — required whenever
  you add or change logging.
- **[analyze-logs](.claude/skills/analyze-logs/SKILL.md)** — sanitize logs
  locally first, then investigate only the sanitized copies (never the raw logs).
- **[log-analysis](.claude/skills/log-analysis/SKILL.md)** — investigate the
  sanitized logs to find the root cause of a reported issue.

## Logging & privacy (privacy-critical)

Proton Drive logs can contain personal data (account email, filenames, paths,
tokens). Two independent guardrails keep it out of Sentry:

1. **At the log site — the `safe-logging` skill.** Any diff that touches a
   `CoreLogger.*` call, a `LogTag` extension, an `Event.Sentry`, a `Throwable`
   passed to a logger, or a new `Timber.Tree` must open and follow
   [.claude/skills/safe-logging/SKILL.md](.claude/skills/safe-logging/SKILL.md):
   never interpolate PII or secrets into a log message, tag, or exception
   message — log a resource ID / error code / status instead. Note that
   **every** Timber level can reach Sentry (`ERROR` as an event, lower levels as
   breadcrumbs attached to the next event), so the rule is not limited to error
   logs.

2. **At the boundary — `PiiScrubber`.** `app/.../log/PiiScrubber.kt` runs in
   `beforeSend` on both Sentry hubs and redacts emails, bearer/auth headers &
   JWTs, URL tokens, contact/display names, message subjects/bodies, user folder
   paths, and `content://` / external-storage paths — across the message,
   exception values, stack frames, breadcrumbs, extras, tags, user identity
   fields (email/username/ip; keeps `id`), and the `sdk_error.log` attachment.
   It is a safety net, **not** a licence to log PII. If a change can emit a
   **new** sensitive category the scrubber doesn't cover, add a pattern to
   `PiiScrubber` and a matching `PiiScrubberTest` case.
