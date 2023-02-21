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
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.receiver.NotificationBroadcastReceiver
import me.proton.android.drive.receiver.NotificationBroadcastReceiver.Companion.ACTION_DELETE
import me.proton.android.drive.receiver.NotificationBroadcastReceiver.Companion.EXTRA_NOTIFICATION_ID
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.util.kotlin.serialize
import javax.inject.Inject

class CreateDeletePendingIntent @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    operator fun invoke(notificationId: NotificationId, notificationEvent: NotificationEvent): PendingIntent? =
        PendingIntent.getBroadcast(
            appContext,
            notificationEvent.deleteRequestCode,
            Intent(appContext, NotificationBroadcastReceiver::class.java).apply {
                action = ACTION_DELETE
                putExtra(EXTRA_NOTIFICATION_ID, notificationId.serialize())
            },
            PendingIntent.FLAG_IMMUTABLE
        )

    private val NotificationEvent.deleteRequestCode: Int get() = when (this) {
        is NotificationEvent.StorageFull -> REQUEST_CODE_STORAGE_FULL
        is NotificationEvent.Upload -> REQUEST_CODE_UPLOAD
        is NotificationEvent.Download -> REQUEST_CODE_DOWNLOAD
    }

    companion object {
        private const val BASE_REQUEST_CODE = 100
        const val REQUEST_CODE_STORAGE_FULL = BASE_REQUEST_CODE + 1
        const val REQUEST_CODE_UPLOAD = BASE_REQUEST_CODE + 2
        const val REQUEST_CODE_DOWNLOAD = BASE_REQUEST_CODE + 3
    }
}
