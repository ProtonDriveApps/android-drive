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
import me.proton.core.drive.backup.data.di.BackupModule
import me.proton.core.drive.backup.data.repository.FolderFindDuplicatesRepository
import me.proton.core.drive.backup.domain.manager.BackupConnectivityManager
import me.proton.core.drive.backup.domain.manager.StubbedBackupConnectivityManager
import me.proton.core.drive.backup.domain.repository.FindDuplicatesRepository
import javax.inject.Singleton

@Module
@Suppress("unused")
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [BackupModule::class]
)
interface TestBackupModule {
    @Binds
    @Singleton
    fun bindsFolderFindDuplicatesRepository(impl: FolderFindDuplicatesRepository): FindDuplicatesRepository
    @Binds
    @Singleton
    fun bindsBackupConnectivityManager(impl: StubbedBackupConnectivityManager): BackupConnectivityManager
}
