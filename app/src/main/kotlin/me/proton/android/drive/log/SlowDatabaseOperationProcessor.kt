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

import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.SentryEvent
import me.proton.core.drive.base.data.db.SlowDatabaseOperationException
import me.proton.core.drive.base.domain.extension.findThrowable

class SlowDatabaseOperationProcessor : EventProcessor {

    override fun process(event: SentryEvent, hint: Hint): SentryEvent {
        event.throwable
            .findThrowable<SlowDatabaseOperationException>()
            ?.enrichEvent(event)
        return event
    }

    private fun SlowDatabaseOperationException.enrichEvent(event: SentryEvent) {
        event.fingerprints = fingerprint
        event.setTag(TAG_DATABASE, database)
        event.setTag(TAG_OPERATION, operation.name.lowercase())
        event.setTag(TAG_MAIN_THREAD, isMainThread.toString())
        event.setTag(TAG_DURATION, durationBucket(durationMs))
        event.setExtra(EXTRA_DURATION_MS, durationMs)
        sql?.let { sql -> event.setExtra(EXTRA_SQL, sql) }
    }

    private fun durationBucket(durationMs: Long): String = when {
        durationMs < 500 -> "<0.5s"
        durationMs < 1_000 -> "0.5-1s"
        durationMs < 2_000 -> "1-2s"
        durationMs < 5_000 -> "2-5s"
        durationMs < 10_000 -> "5-10s"
        else -> ">=10s"
    }

    private companion object {
        const val TAG_DATABASE = "db_name"
        const val TAG_OPERATION = "db_operation"
        const val TAG_MAIN_THREAD = "db_main_thread"
        const val TAG_DURATION = "db_duration"
        const val EXTRA_DURATION_MS = "db_duration_ms"
        const val EXTRA_SQL = "db_sql"
    }
}
