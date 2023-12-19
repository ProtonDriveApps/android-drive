/*
 * Copyright (c) 2023 Proton AG.
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
import me.proton.core.drive.backup.data.handler.UploadErrorHandlerImpl
import me.proton.core.drive.backup.data.manager.BackupManagerImpl
import me.proton.core.drive.backup.data.manager.BackupPermissionsManagerImpl
import me.proton.core.drive.backup.data.repository.BackupDuplicateRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.data.repository.ContextCountLibraryItemsRepository
import me.proton.core.drive.backup.data.repository.ContextBucketRepository
import me.proton.core.drive.backup.data.repository.ContextScanFolderRepository
import me.proton.core.drive.backup.data.worker.BackupCleanupWorkers
import me.proton.core.drive.backup.domain.handler.UploadErrorHandler
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.backup.domain.repository.BackupDuplicateRepository
import me.proton.core.drive.backup.domain.repository.BackupErrorRepository
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.backup.domain.repository.BackupFolderRepository
import me.proton.core.drive.backup.domain.repository.BucketRepository
import me.proton.core.drive.backup.domain.repository.CountLibraryItemsRepository
import me.proton.core.drive.backup.domain.repository.ScanFolderRepository
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
    fun bindsBackupManager(impl: BackupManagerImpl): BackupManager

    @Binds
    fun bindsScanFolder(impl: ContextScanFolderRepository): ScanFolderRepository

    @Binds
    fun bindsCleanUpWorkers(impl: BackupCleanupWorkers): CleanupWorkers

    @Binds
    @Singleton
    fun bindBackupPermissionsManager(impl: BackupPermissionsManagerImpl): BackupPermissionsManager

    @Binds
    @Singleton
    fun bindUploadErrorHandler(impl: UploadErrorHandlerImpl): UploadErrorHandler

    @Binds
    @Singleton
    fun bindBucketRepository(impl: ContextBucketRepository): BucketRepository

    @Binds
    @Singleton
    fun bindCountLibraryItemsRepository(impl: ContextCountLibraryItemsRepository): CountLibraryItemsRepository

}
