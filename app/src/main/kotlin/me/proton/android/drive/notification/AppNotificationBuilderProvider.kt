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

package me.proton.android.drive.notification

import androidx.core.app.NotificationCompat
import me.proton.android.drive.usecase.notification.BackupNotificationBuilder
import me.proton.android.drive.usecase.notification.DownloadNotificationBuilder
import me.proton.android.drive.usecase.notification.ForcedSignOutNotificationBuilder
import me.proton.android.drive.usecase.notification.NoSpaceLeftOnDeviceNotificationBuilder
import me.proton.android.drive.usecase.notification.StorageFullNotificationBuilder
import me.proton.android.drive.usecase.notification.UploadNotificationBuilder
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.domain.extension.requireIsInstance
import me.proton.core.drive.notification.data.provider.NotificationBuilderProvider
import me.proton.core.drive.notification.domain.entity.NotificationId
import javax.inject.Inject

class AppNotificationBuilderProvider @Inject constructor(
    private val storageFullBuilder: StorageFullNotificationBuilder,
    private val uploadNotificationBuilder: UploadNotificationBuilder,
    private val downloadNotificationBuilder: DownloadNotificationBuilder,
    private val forcedSignOutNotificationBuilder: ForcedSignOutNotificationBuilder,
    private val noSpaceLeftOnDeviceNotificationBuilder: NoSpaceLeftOnDeviceNotificationBuilder,
    private val backupNotificationBuilder: BackupNotificationBuilder,
) : NotificationBuilderProvider {

    @Suppress("UNCHECKED_CAST")
    override fun get(
        notificationId: NotificationId,
        events: List<Event>,
    ): NotificationCompat.Builder = when {
        events.size == 1 && events.first() is Event.StorageFull ->
            storageFullBuilder(
                notificationId = requireIsInstance(notificationId),
                event = events.first() as Event.StorageFull,
            )
        events.isNotEmpty() && events.all { event -> event is Event.Upload } ->
            uploadNotificationBuilder(
                notificationId = requireIsInstance(notificationId),
                events = events as List<Event.Upload>,
            )
        events.size == 1 && events.first() is Event.Download ->
            downloadNotificationBuilder(
                notificationId = requireIsInstance(notificationId),
                event = events.first() as Event.Download,
            )
        events.size == 1 && events.first() is Event.ForcedSignOut ->
            forcedSignOutNotificationBuilder(
                notificationId = requireIsInstance(notificationId),
                event = events.first() as Event.ForcedSignOut,
            )
        events.size == 1 && events.first() is Event.NoSpaceLeftOnDevice ->
            noSpaceLeftOnDeviceNotificationBuilder(
                notificationId = requireIsInstance(notificationId),
                event = events.first() as Event.NoSpaceLeftOnDevice,
            )
        events.size == 1 && events.first() is Event.Backup ->
            backupNotificationBuilder(
                notificationId = requireIsInstance(notificationId),
                event = events.first() as Event.Backup,
            )
        else -> error("Unhandled notification events")
    }
}
