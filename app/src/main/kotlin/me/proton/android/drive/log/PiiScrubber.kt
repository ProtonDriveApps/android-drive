/*
 * Copyright (c) 2026 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.log

import io.sentry.Attachment
import io.sentry.Hint
import io.sentry.SentryEvent

/**
 * Last-chance redaction of PII / secrets from everything sent to Sentry.
 *
 * Sentry is an external system, so nothing tied to a person (see the
 * `safe-logging` skill for Proton's data classification) may reach it. This is
 * a defence-in-depth net that runs in `beforeSend`, after every [io.sentry.EventProcessor]
 * (including the enricher that attaches `sdk_error.log`), so no field added
 * upstream escapes. It is NOT a substitute for not logging PII in the first
 * place — the skill checklist still applies at every log site.
 *
 * The patterns mirror the offline iOS/macOS sanitizer taxonomy. Opaque values
 * that Proton classifies as safe to log (resource IDs, node/upload IDs, UUIDs,
 * encrypted blobs, content hashes) are intentionally NOT matched — they are
 * exactly what we need to debug and reveal nothing without backend access.
 */
object PiiScrubber {

    private const val REDACTED = "<redacted>"
    private const val REDACTED_PROTON_EMAIL = "<redacted_proton_email>"
    private const val REDACTED_EXTERNAL_EMAIL = "<redacted_external_email>"

    /** Distinguish a Proton account email from an external contact email. */
    private val PROTON_EMAIL_DOMAINS = setOf(
        "proton.me", "protonmail.com", "protonmail.ch", "pm.me", "proton.black",
    )

    /**
     * Personal identifier: account / contact email address.
     * `\b` anchors the start so a long run of local-part chars can't re-anchor at
     * every position, and `++` (possessive) stops the local part from
     * backtracking — together these keep matching linear on adversarial input
     * like "aaaa…@" (which was O(n²) without them).
     */
    private val EMAIL = Regex("""\b[A-Za-z0-9._%+\-]++@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")

    /** Plaintext credential in an `Authorization: Bearer <token>` form. */
    private val BEARER = Regex("""(?i)\bBearer\s+[A-Za-z0-9._\-]+""")

    /**
     * Bare JWT / signed token (`header.payload[.signature]`) — may embed claims
     * such as the account email or `sub`. `eyJ` is base64url of `{"`.
     */
    private val JWT = Regex("""\beyJ[A-Za-z0-9_-]{5,}+\.[A-Za-z0-9_-]{5,}+(?:\.[A-Za-z0-9_-]++)?""")

    /**
     * Personal fields in mail / sharing payloads — contact & display names
     * (`Name`, `SenderName`, …) and message content (`Subject`, `Body`). Keeps
     * the key, redacts the value. Matches `'Key':'value'` and `"Key":"value"`.
     */
    private val PERSONAL_FIELD = Regex(
        """(['"](?:Name|SenderName|DisplayName|RecipientName|ContactName|Subject|Body)['"]\s*:\s*)""" +
            """(['"]).*?\2"""
    )

    /**
     * `Path` is a user folder / label path (customer content, e.g.
     * "Achats \/ Abonnements"). A value starting with `/` is an API route or
     * filesystem path — those are NOT PII and stay, for debugging. The `(?!/)`
     * after the opening quote makes the distinction.
     */
    private val LABEL_PATH_FIELD = Regex("""(['"]Path['"]\s*:\s*)(['"])(?!/).*?\2""")

    /**
     * Sensitive HTTP headers as printed by loggers (`Header: value`). Keeps the
     * header name and redacts the whole value to end of line — header values can
     * contain spaces (`Bearer <token>`) or continuations (`a=b; c=d`), so a
     * whitespace-bounded match would leak the tail.
     */
    private val SENSITIVE_HEADER = Regex(
        """(?i)\b(authorization|proton-authorization|cookie|set-cookie|""" +
            """x-pm-uid|x-pm-human|x-pm-appversion-token)(\s*[:=]\s*)[^\r\n]+"""
    )

    /** Credential / recovery tokens carried in a URL query string. Keeps the key. */
    private val URL_TOKEN = Regex(
        """(?i)([?&](?:token|access_token|refresh_token|code|password)=)[^\s&"')]+"""
    )

    /**
     * Absolute paths and content URIs that embed user filenames (customer
     * content). Internal app paths under `/data/data` are left alone.
     */
    private val CONTENT_URI = Regex("""content://[^\s"')]+""")
    private val STORAGE_PATH = Regex("""/(?:storage/emulated/\d+|sdcard|mnt/sdcard)/[^\s"')]+""")

    /** Redact PII / secrets from a free-text string. Safe to call on any log line. */
    fun scrubText(text: String): String =
        text
            .replace(SENSITIVE_HEADER) { "${it.groupValues[1]}${it.groupValues[2]}$REDACTED" }
            .replace(JWT, REDACTED)
            .replace(BEARER, "Bearer $REDACTED")
            .replace(URL_TOKEN) { "${it.groupValues[1]}$REDACTED" }
            .replace(PERSONAL_FIELD) { "${it.groupValues[1]}${it.groupValues[2]}$REDACTED${it.groupValues[2]}" }
            .replace(LABEL_PATH_FIELD) { "${it.groupValues[1]}${it.groupValues[2]}$REDACTED${it.groupValues[2]}" }
            .replace(EMAIL) { redactEmail(it.value) }
            .replace(CONTENT_URI) { redactFilename(it.value) }
            .replace(STORAGE_PATH) { redactFilename(it.value) }

    /** Proton account email vs external contact email — different token, no value kept. */
    private fun redactEmail(email: String): String {
        val domain = email.substringAfterLast('@', "").lowercase()
        val isProton = PROTON_EMAIL_DOMAINS.any { domain == it || domain.endsWith(".$it") }
        return if (isProton) REDACTED_PROTON_EMAIL else REDACTED_EXTERNAL_EMAIL
    }

    /** Keep the directory (useful for debugging), redact only the filename (customer content). */
    private fun redactFilename(path: String): String {
        val slash = path.lastIndexOf('/')
        return if (slash in 0 until path.length - 1) path.substring(0, slash + 1) + REDACTED else REDACTED
    }

    /** Scrub every PII-bearing surface of a Sentry event and its hint in place. */
    fun scrub(event: SentryEvent, hint: Hint): SentryEvent {
        event.message?.let { message ->
            message.formatted = message.formatted?.let(::scrubText)
            message.message = message.message?.let(::scrubText)
        }

        event.exceptions?.forEach { exception ->
            exception.value = exception.value?.let(::scrubText)
            exception.stacktrace?.frames?.forEach { frame ->
                frame.filename = frame.filename?.let(::scrubText)
                frame.contextLine = frame.contextLine?.let(::scrubText)
            }
        }

        event.breadcrumbs?.forEach { breadcrumb ->
            breadcrumb.message = breadcrumb.message?.let(::scrubText)
            scrubStringValues(breadcrumb.data)
        }

        event.extras?.toMap()?.forEach { (key, value) ->
            if (value is String) event.setExtra(key, scrubText(value))
        }

        // Tags are user-visible and searchable; safe-logging requires them to be
        // non-personal. Snapshot before mutating (setTag writes the same map).
        event.tags?.toMap()?.forEach { (key, value) -> event.setTag(key, scrubText(value)) }

        // Identity fields are PII by definition — redact wholesale (a bare
        // username / device name matches no free-text pattern). Keep user.id: it
        // is an opaque resource ID and the main correlation handle.
        event.user?.let { user ->
            user.email?.let { user.email = if (EMAIL.matches(it)) redactEmail(it) else REDACTED }
            if (user.username != null) user.username = REDACTED
            if (user.ipAddress != null) user.ipAddress = REDACTED
        }
        event.contexts.device?.let { device ->
            if (device.name != null) device.name = REDACTED
        }

        scrubAttachments(hint)
        return event
    }

    private fun scrubStringValues(data: MutableMap<String, Any>?) {
        data ?: return
        data.entries.forEach { entry ->
            (entry.value as? String)?.let { entry.setValue(scrubText(it)) }
        }
    }

    private fun scrubAttachments(hint: Hint) {
        val attachments = hint.attachments
        if (attachments.isEmpty()) return
        val scrubbed = attachments.mapNotNull { attachment ->
            val bytes = attachment.bytes
            if (bytes == null) {
                // Path-based attachment (e.g. screenshot / view-hierarchy / trace):
                // its contents are a file we can't inspect or scrub here, so drop
                // it rather than forward it unscrubbed. Today's only attachment
                // (sdk_error.log) is byte-based and handled below.
                null
            } else {
                // Preserve attachmentType. addToTransactions can't be read back
                // (its getter is package-private in the SDK), so it falls back to
                // the SDK default (false) — which is also what our only attachment,
                // sdk_error.log, is created with.
                Attachment(
                    scrubText(String(bytes)).toByteArray(),
                    attachment.filename,
                    attachment.contentType,
                    attachment.attachmentType,
                    false,
                )
            }
        }
        hint.replaceAttachments(scrubbed)
    }
}
