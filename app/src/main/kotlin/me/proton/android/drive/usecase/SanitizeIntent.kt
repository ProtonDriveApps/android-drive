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

package me.proton.android.drive.usecase

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.receiver.NotificationBroadcastReceiver.Companion.EXTRA_NOTIFICATION_ID
import me.proton.android.drive.ui.MainActivity
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class SanitizeIntent @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val component = ComponentName(appContext, MainActivity::class.java)

    operator fun invoke(intent: Intent): Intent = when (intent.action) {
        Intent.ACTION_VIEW -> intent.data
            ?.takeIf { uri -> uri.isOwnDeepLink }
            ?.let { uri -> Intent(Intent.ACTION_VIEW, uri).setComponent(component) }
        Intent.ACTION_SEND -> Intent(Intent.ACTION_SEND).apply {
            setComponent(component)
            type = intent.type
            clipData = intent.clipData
            getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { uri ->
                putExtra(Intent.EXTRA_STREAM, uri)
            }
        }
        Intent.ACTION_SEND_MULTIPLE -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            setComponent(component)
            type = intent.type
            clipData = intent.clipData
            getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { uris ->
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }
        else -> null
    }
        .let { sanitized -> sanitized ?: Intent.makeMainActivity(component) }
        .apply {
            flags = intent.flags
            intent.getStringExtra(EXTRA_NOTIFICATION_ID)?.let { notificationId ->
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
        }

    private val Uri.isOwnDeepLink: Boolean get() =
        scheme == appContext.getString(I18N.string.deeplink_scheme) &&
                host == appContext.getString(I18N.string.deeplink_host)
}
