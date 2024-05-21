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

package me.proton.core.drive.backup.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import me.proton.core.drive.backup.data.di.BackupContextBindModule
import me.proton.core.drive.backup.data.repository.ContextCountLibraryItemsRepository
import me.proton.core.drive.backup.domain.repository.BucketRepository
import me.proton.core.drive.backup.domain.repository.CountLibraryItemsRepository
import me.proton.core.drive.backup.domain.repository.ScanFolderRepository
import me.proton.core.drive.backup.domain.repository.TestBucketRepository
import me.proton.core.drive.backup.domain.repository.TestScanFolderRepository
import javax.inject.Singleton

@Module
@Suppress("unused")
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [BackupContextBindModule::class]
)
interface TestBackupContextBindModule {

    @Binds
    fun bindsScanFolder(impl: TestScanFolderRepository): ScanFolderRepository

    @Binds
    @Singleton
    fun bindBucketRepository(impl: TestBucketRepository): BucketRepository

    @Binds
    @Singleton
    fun bindCountLibraryItemsRepository(impl: ContextCountLibraryItemsRepository): CountLibraryItemsRepository

}
