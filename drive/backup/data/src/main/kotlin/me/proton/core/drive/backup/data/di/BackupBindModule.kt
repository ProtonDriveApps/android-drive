/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.backup.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import me.proton.core.drive.backup.data.handler.BackupUploadErrorHandler
import me.proton.core.drive.backup.data.manager.BackupPermissionsManagerImpl
import me.proton.core.drive.backup.data.provider.BackupNetworkTypeProvider
import me.proton.core.drive.backup.data.repository.BackupConfigurationRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupDuplicateRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.data.worker.BackupCleanupWorkers
import me.proton.core.drive.upload.domain.handler.UploadErrorHandler
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.backup.domain.repository.BackupConfigurationRepository
import me.proton.core.drive.backup.domain.repository.BackupDuplicateRepository
import me.proton.core.drive.backup.domain.repository.BackupErrorRepository
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.backup.domain.repository.BackupFolderRepository
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.upload.data.di.UploadModule
import me.proton.core.drive.upload.data.provider.NetworkTypeProvider
import me.proton.core.drive.upload.data.worker.CleanupWorkers
import javax.inject.Singleton

@Module
@Suppress("unused")
@InstallIn(SingletonComponent::class)
interface BackupBindModule {
    @Binds
    @Singleton
    fun bindsBackupRepository(impl: BackupFileRepositoryImpl): BackupFileRepository

    @Binds
    @Singleton
    fun bindsBackupFolderRepository(impl: BackupFolderRepositoryImpl): BackupFolderRepository

    @Binds
    @Singleton
    fun bindsBackupErrorRepository(impl: BackupErrorRepositoryImpl): BackupErrorRepository

    @Binds
    @Singleton
    fun bindsBackupDuplicateRepository(impl: BackupDuplicateRepositoryImpl): BackupDuplicateRepository

    @Binds
    @Singleton
    fun bindsBackupConfigurationRepository(impl: BackupConfigurationRepositoryImpl): BackupConfigurationRepository

    @Binds
    fun bindsCleanUpWorkers(impl: BackupCleanupWorkers): CleanupWorkers

    @Binds
    @Singleton
    fun bindBackupPermissionsManager(impl: BackupPermissionsManagerImpl): BackupPermissionsManager

    @Binds
    @IntoSet
    fun bindUploadErrorHandler(impl: BackupUploadErrorHandler): UploadErrorHandler

    @Binds
    @IntoMap
    @Singleton
    @UploadModule.NetworkTypeProviderKey(NetworkTypeProviderType.BACKUP)
    fun provideBackupNetworkTypeProvider(impl: BackupNetworkTypeProvider): NetworkTypeProvider

}
