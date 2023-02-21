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

package me.proton.android.drive.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.messagequeue.ApplicationActionProvider
import me.proton.core.drive.messagequeue.data.MessageQueueImpl
import me.proton.core.drive.messagequeue.data.storage.RoomStorage
import me.proton.core.drive.messagequeue.data.storage.db.MessageQueueDatabase
import me.proton.core.drive.messagequeue.domain.ActionProvider
import me.proton.core.drive.messagequeue.domain.MessageQueue
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
object MessageQueueModule {

    @Provides
    @Singleton
    fun providesMessageQueue(database: MessageQueueDatabase): MessageQueue<BroadcastMessage> =
        MessageQueueImpl(RoomStorage(database))

    @Provides
    @Singleton
    fun providesActionProvider(applicationActionProvider: ApplicationActionProvider): ActionProvider =
        applicationActionProvider
}
