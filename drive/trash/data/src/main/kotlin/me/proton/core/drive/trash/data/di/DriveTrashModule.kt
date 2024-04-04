/*
 * Copyright (c) 2021-2024 Proton AG.
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

package me.proton.core.drive.trash.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.drive.messagequeue.domain.ActionProvider
import me.proton.core.drive.trash.data.repository.DriveTrashRepositoryImpl
import me.proton.core.drive.trash.domain.notification.TrashExtraActionProvider
import me.proton.core.drive.trash.domain.repository.DriveTrashRepository

@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface DriveTrashModule {

    @Binds
    fun bindDriveTrashRepository(repository: DriveTrashRepositoryImpl): DriveTrashRepository

    @Binds
    @IntoSet
    fun bindsActionProvider(provider: TrashExtraActionProvider): ActionProvider
}
