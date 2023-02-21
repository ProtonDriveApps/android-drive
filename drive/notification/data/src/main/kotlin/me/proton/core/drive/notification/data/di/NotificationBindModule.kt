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
package me.proton.core.drive.notification.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.notification.data.manager.NotificationManagerImpl
import me.proton.core.drive.notification.data.repository.NotificationRepositoryImpl
import me.proton.core.drive.notification.domain.manager.NotificationManager
import me.proton.core.drive.notification.domain.repository.NotificationRepository
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface NotificationBindModule {

    @Binds
    @Singleton
    fun bindsNotificationRepositoryImpl(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    fun bindsNotificationManagerImpl(impl: NotificationManagerImpl): NotificationManager
}
