# AGENTS.md

Guidance for agents working in this repository.

## Skills

Skills live in the committed **`.claude/skills/<name>/SKILL.md`** directory —
a plain, transparent location everyone can see and review. Add a new shared
skill by creating `.claude/skills/<name>/SKILL.md` (plus any bundled scripts)
and committing it. Personal, uncommitted skills can go in
`~/.claude/skills/<name>/` (all your projects).

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
