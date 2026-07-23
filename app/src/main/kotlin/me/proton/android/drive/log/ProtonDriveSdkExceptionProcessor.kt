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
import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.SentryEvent
import me.proton.core.drive.base.domain.extension.findThrowable
import me.proton.drive.sdk.ProtonDriveSdkException
import me.proton.drive.sdk.errorToString

class ProtonDriveSdkExceptionProcessor : EventProcessor {

    override fun process(event: SentryEvent, hint: Hint): SentryEvent {
        event.throwable
            .findThrowable<ProtonDriveSdkException>()
            ?.enrichEvent(event, hint)
        return event
    }

    private fun ProtonDriveSdkException.enrichEvent(
        event: SentryEvent,
        hint: Hint
    ) {
        error?.let { error ->
            event.setTag("sdk_error_type", error.type)
            event.setTag("sdk_error_domain", error.domain.toString())
            error.primaryCode?.let { code ->
                event.setTag("sdk_error_primary_code", code.toString())
            }
            error.secondaryCode?.let { code ->
                event.setTag("sdk_error_secondary_code", code.toString())
            }
            hint.addAttachment(
                Attachment(
                    errorToString().toByteArray(),
                    "sdk_error.log",
                    "text/plain"
                )
            )
        }
    }
}
