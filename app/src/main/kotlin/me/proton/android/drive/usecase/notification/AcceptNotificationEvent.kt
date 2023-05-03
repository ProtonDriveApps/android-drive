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

class AcceptNotificationEvent @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(notificationId: NotificationId, newEvent: NotificationEvent): Boolean =
        when (newEvent) {
            is NotificationEvent.StorageFull -> true
            is NotificationEvent.Upload -> notificationRepository.getNotificationEvent(
                notificationId = requireIsInstance(notificationId),
                notificationEventId = newEvent.id,
            ).let { oldEvent ->
                oldEvent != null || newEvent.state == UploadState.NEW_UPLOAD
            }
            is NotificationEvent.Download -> true
            is NotificationEvent.ForcedSignOut -> true
            is NotificationEvent.NoSpaceLeftOnDevice -> true
        }
}
