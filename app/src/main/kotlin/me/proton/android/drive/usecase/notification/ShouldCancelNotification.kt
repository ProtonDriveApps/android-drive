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

import me.proton.core.drive.base.domain.extension.requireIsInstance
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.entity.NotificationEvent.Upload.UploadState
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.notification.domain.repository.NotificationRepository
import javax.inject.Inject

class ShouldCancelNotification @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(notificationId: NotificationId, event: NotificationEvent): Boolean = when (event) {
        is NotificationEvent.StorageFull -> false
        is NotificationEvent.Upload -> {
            val uploadEvents = notificationRepository.getAllNotificationEvents(
                notificationId = requireIsInstance(notificationId),
            )
                .filterIsInstance<NotificationEvent.Upload>()
            (uploadEvents.isEmpty() || uploadEvents.allFinished()) && event.state == UploadState.UPLOAD_CANCELLED
        }
        is NotificationEvent.Download -> false
        is NotificationEvent.ForcedSignOut -> false
        is NotificationEvent.NoSpaceLeftOnDevice -> false
    }

    private fun List<NotificationEvent.Upload>.allFinished(): Boolean = all { uploadEvent ->
        uploadEvent.state in setOf(UploadState.UPLOAD_COMPLETE, UploadState.UPLOAD_FAILED, UploadState.UPLOAD_CANCELLED)
    }
}
