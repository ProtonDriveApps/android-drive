/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.files.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.drive.files.data.operation.FileOperationManagerImpl
import me.proton.core.drive.files.domain.operation.FileOperationActionProvider
import me.proton.core.drive.files.domain.operation.FileOperationManager
import me.proton.core.drive.messagequeue.domain.ActionProvider

@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface FilesListModule {

    @Binds
    fun bindsFileOperationManager(manager: FileOperationManagerImpl): FileOperationManager

    @Binds
    @IntoSet
    fun bindsActionProvider(provider: FileOperationActionProvider): ActionProvider
}
