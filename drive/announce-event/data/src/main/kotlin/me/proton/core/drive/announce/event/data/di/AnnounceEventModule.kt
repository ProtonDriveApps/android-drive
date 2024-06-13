/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.announce.event.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.announce.event.domain.handler.EventHandler
import me.proton.core.drive.announce.event.domain.usecase.AsyncAnnounceEvent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnnounceEventModule {

    @Provides
    @Singleton
    fun provideAsyncAnnounceEvent(
        eventHandlers: @JvmSuppressWildcards Set<EventHandler>,
    ): AsyncAnnounceEvent =
        AsyncAnnounceEvent(
            eventHandlers = eventHandlers,
            coroutineContext = Job() + Dispatchers.IO,
        )
}
