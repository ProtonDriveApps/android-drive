# AGENTS.md

Guidance for agents working in this repository. This file is always loaded; the detailed
documentation lives under [docs/agents/](docs/agents/README.md) and is read on demand.
`FLANDERS.md` defines the MR review bot's mandate (when asked, it reviews non-draft MRs against this
file and the `docs/agents/` documents it links, in particular `docs/agents/conventions/general.md`).

## Before planning a change

Any change that will end in an MR starts by reading [docs/agents/README.md](docs/agents/README.md)
and the documents it lists for the kind of change you are making. Plans must fit the architecture
described there.

## Before opening an MR

Every MR is preceded by a code review and a security review by fresh-context subagents, judged
against the change's requirements: run the [pre-mr-review](.claude/skills/pre-mr-review/SKILL.md)
skill on the committed branch and include its summary in the MR description.

Agents never merge MRs; a developer merges in GitLab after CI and human review.

In Claude Code, the `.claude/hooks/mr_guard.py` hook enforces both: it blocks opening an MR (through
`glab`, the GitLab API or push options) until a review is recorded for the current HEAD, and always
blocks merging. It matches the raw command text, so a command that only mentions these operations,
such as a commit message, is blocked too: put that text in a file (`git commit -F <file>`). It
guards against accidents, not deliberate evasion (its docstring lists what it does not cover), and
needs `python3`; CI, Flanders and human review remain the real gates. Its tests run locally, not in
CI: `python3 -m unittest discover -s .claude/hooks`.

Flanders does not start on its own, and it does not review draft MRs. Once the MR is out of draft,
ask for its review with an MR comment `@sa-devex-Flanders review please` or by requesting a review
from `sa-devex-Flanders`. Ask again after pushing fixes for its comments: a new commit resets its
approval.

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
`.claude/settings.json` is committed and shared (it registers the MR hook); personal Claude Code
settings, permissions and any secrets go in the git-ignored `.claude/settings.local.json`.

Proton Core modules are pulled in by the `include-core-build` settings plugin, not vendored here.
The Drive SDK (`me.proton.drive:sdk`) is an external artifact; set `me.proton.drive.sdk.path` in
`gradle.properties` to substitute a local composite build. `dev`/`alpha` resolve the `-rust` crypto
SDK variant, `beta`/`prod` the Go one — this is forced per-variant in `app/build.gradle.kts`.

## Conventions

Repository and team conventions apply to every change and live in
[docs/agents/conventions/general.md](docs/agents/conventions/general.md); read it before planning
or reviewing a change.

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
- **[release-changelog](.claude/skills/release-changelog/SKILL.md)** — changelog
  of everything not yet published in a GitLab release.
- **[release-announce](.claude/skills/release-announce/SKILL.md)** — Slack
  release announcement for `candidate`, copied to the clipboard.
- **[pre-mr-review](.claude/skills/pre-mr-review/SKILL.md)** — fresh-context
  code and security review against the requirements before opening any MR
  (enforced by a hook).

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
