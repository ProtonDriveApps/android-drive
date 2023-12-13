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
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.util.kotlin.serialize
import javax.inject.Inject

class CreateDeletePendingIntent @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    operator fun invoke(notificationId: NotificationId, notificationEvent: Event): PendingIntent? =
        PendingIntent.getBroadcast(
            appContext,
            notificationEvent.deleteRequestCode,
            Intent(appContext, NotificationBroadcastReceiver::class.java).apply {
                action = ACTION_DELETE
                putExtra(EXTRA_NOTIFICATION_ID, notificationId.serialize())
            },
            PendingIntent.FLAG_IMMUTABLE
        )

    private val Event.deleteRequestCode: Int get() = when (this) {
        is Event.StorageFull -> REQUEST_CODE_STORAGE_FULL
        is Event.Upload -> REQUEST_CODE_UPLOAD
        is Event.Download -> REQUEST_CODE_DOWNLOAD
        is Event.ForcedSignOut -> REQUEST_CODE_FORCED_SIGN_OUT
        is Event.NoSpaceLeftOnDevice -> REQUEST_CODE_NO_SPACE_LEFT_ON_DEVICE
        is Event.Backup -> REQUEST_CODE_BACKUP
        else -> 0
    }

    companion object {
        private const val BASE_REQUEST_CODE = 100
        const val REQUEST_CODE_STORAGE_FULL = BASE_REQUEST_CODE + 1
        const val REQUEST_CODE_UPLOAD = BASE_REQUEST_CODE + 2
        const val REQUEST_CODE_DOWNLOAD = BASE_REQUEST_CODE + 3
        const val REQUEST_CODE_FORCED_SIGN_OUT = BASE_REQUEST_CODE + 4
        const val REQUEST_CODE_NO_SPACE_LEFT_ON_DEVICE = BASE_REQUEST_CODE + 5
        const val REQUEST_CODE_BACKUP = BASE_REQUEST_CODE + 6
    }
}
