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

import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.entity.Event.Upload.UploadState
import me.proton.core.drive.base.domain.extension.requireIsInstance
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.notification.domain.repository.NotificationRepository
import javax.inject.Inject

class AcceptNotificationEvent @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(notificationId: NotificationId, newEvent: Event): Boolean =
        when (newEvent) {
            is Event.StorageFull -> true
            is Event.Upload -> newEvent.shouldShow && notificationRepository.getNotificationEvent(
                notificationId = requireIsInstance(notificationId),
                notificationEventId = newEvent.id,
            ).let { oldEvent ->
                oldEvent != null || newEvent.state == UploadState.NEW_UPLOAD
            }

            is Event.Download -> true
            is Event.ForcedSignOut -> true
            is Event.NoSpaceLeftOnDevice -> true
            is Event.Backup -> true
            else -> false
        }
}
