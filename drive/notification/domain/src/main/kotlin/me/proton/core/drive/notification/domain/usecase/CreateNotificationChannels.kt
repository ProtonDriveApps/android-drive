/*
 * Copyright (c) 2022-2023 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.core.drive.notification.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.notification.domain.entity.Channel
import me.proton.core.drive.notification.domain.manager.NotificationManager
import me.proton.core.drive.notification.domain.repository.NotificationRepository
import javax.inject.Inject

class CreateNotificationChannels @Inject constructor(
    private val notificationManager: NotificationManager,
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(
        userId: UserId,
        username: String,
        channels: List<Channel.User> = listOf(Channel.User(userId, Channel.Type.TRANSFER))
    ) {
        notificationManager.createUserChannels(userId, username, channels)
        notificationRepository.insertChannels(channels)
    }

    operator fun invoke(
        name: String,
        channels: List<Channel.App> = listOf(Channel.App(Channel.Type.WARNING))
    ) {
        notificationManager.createAppChannels(name, channels)
    }
}
