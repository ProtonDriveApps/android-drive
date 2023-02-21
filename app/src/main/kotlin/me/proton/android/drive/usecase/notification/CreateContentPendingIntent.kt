/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.usecase.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.TaskStackBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.receiver.NotificationBroadcastReceiver.Companion.EXTRA_NOTIFICATION_ID
import me.proton.android.drive.ui.MainActivity
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.util.kotlin.serialize
import javax.inject.Inject

class CreateContentPendingIntent @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    operator fun invoke(notificationId: NotificationId, uri: Uri): PendingIntent? =
        TaskStackBuilder.create(appContext).run {
            addNextIntentWithParentStack(
                Intent(
                    Intent.ACTION_VIEW,
                    uri,
                    appContext,
                    MainActivity::class.java
                ).apply {
                    putExtra(EXTRA_NOTIFICATION_ID, notificationId.serialize())
                }
            )
            getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE)
        }
}
