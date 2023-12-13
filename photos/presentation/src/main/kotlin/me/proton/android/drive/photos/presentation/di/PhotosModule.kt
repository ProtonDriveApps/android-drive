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

package me.proton.android.drive.photos.presentation.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.android.drive.photos.domain.usecase.TogglePhotosBackup
import me.proton.android.drive.photos.presentation.viewmodel.BackupPermissionsViewModel
import me.proton.android.drive.photos.presentation.viewmodel.BackupPermissionsViewModelImpl
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PhotosModule {

    @Singleton
    @Provides
    fun provideBackupPermissionsViewModel(
        @ApplicationContext appContext: Context,
        backupPermissionsManager: BackupPermissionsManager,
        togglePhotosBackup: TogglePhotosBackup,
        configurationProvider: ConfigurationProvider,
        broadcastMessages: BroadcastMessages,
    ): BackupPermissionsViewModel =
        BackupPermissionsViewModelImpl(
            appContext = appContext,
            backupPermissionsManager = backupPermissionsManager,
            togglePhotosBackup = togglePhotosBackup,
            configurationProvider = configurationProvider,
            broadcastMessages = broadcastMessages,
            coroutineContext = Job() + Dispatchers.Main,
        )
}
