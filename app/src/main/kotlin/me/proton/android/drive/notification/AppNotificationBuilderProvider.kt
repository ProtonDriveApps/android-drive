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
import me.proton.android.drive.usecase.notification.DownloadNotificationBuilder
import me.proton.android.drive.usecase.notification.ForcedSignOutNotificationBuilder
import me.proton.android.drive.usecase.notification.NoSpaceLeftOnDeviceNotificationBuilder
import me.proton.android.drive.usecase.notification.StorageFullNotificationBuilder
import me.proton.android.drive.usecase.notification.UploadNotificationBuilder
import me.proton.core.drive.base.domain.extension.requireIsInstance
import me.proton.core.drive.notification.data.provider.NotificationBuilderProvider
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.entity.NotificationId
import javax.inject.Inject

class AppNotificationBuilderProvider @Inject constructor(
    private val storageFullBuilder: StorageFullNotificationBuilder,
    private val uploadNotificationBuilder: UploadNotificationBuilder,
    private val downloadNotificationBuilder: DownloadNotificationBuilder,
    private val forcedSignOutNotificationBuilder: ForcedSignOutNotificationBuilder,
    private val noSpaceLeftOnDeviceNotificationBuilder: NoSpaceLeftOnDeviceNotificationBuilder,
) : NotificationBuilderProvider {

    @Suppress("UNCHECKED_CAST")
    override fun get(
        notificationId: NotificationId,
        notificationEvents: List<NotificationEvent>,
    ): NotificationCompat.Builder = when {
        notificationEvents.size == 1 && notificationEvents.first() is NotificationEvent.StorageFull ->
            storageFullBuilder(
                notificationId = requireIsInstance(notificationId),
                notificationEvent = notificationEvents.first() as NotificationEvent.StorageFull,
            )
        notificationEvents.isNotEmpty() && notificationEvents.all { event -> event is NotificationEvent.Upload } ->
            uploadNotificationBuilder(
                notificationId = requireIsInstance(notificationId),
                notificationEvents = notificationEvents as List<NotificationEvent.Upload>,
            )
        notificationEvents.size == 1 && notificationEvents.first() is NotificationEvent.Download ->
            downloadNotificationBuilder(
                notificationId = requireIsInstance(notificationId),
                notificationEvent = notificationEvents.first() as NotificationEvent.Download,
            )
        notificationEvents.size == 1 && notificationEvents.first() is NotificationEvent.ForcedSignOut ->
            forcedSignOutNotificationBuilder(
                notificationId = requireIsInstance(notificationId),
                notificationEvent = notificationEvents.first() as NotificationEvent.ForcedSignOut,
            )
        notificationEvents.size == 1 && notificationEvents.first() is NotificationEvent.NoSpaceLeftOnDevice ->
            noSpaceLeftOnDeviceNotificationBuilder(
                notificationId = requireIsInstance(notificationId),
                notificationEvent = notificationEvents.first() as NotificationEvent.NoSpaceLeftOnDevice,
            )
        else -> error("Unhandled notification events")
    }
}
