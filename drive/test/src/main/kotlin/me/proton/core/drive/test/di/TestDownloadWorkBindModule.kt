/*
 * Copyright (c) 2021-2025 Proton AG.
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
package me.proton.core.drive.test.di

import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import me.proton.core.drive.drivelink.download.data.di.DownloadWorkBindModule
import me.proton.core.drive.drivelink.download.domain.manager.DownloadWorkManager
import me.proton.core.drive.test.manager.TestDownloadWorkManager
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DownloadWorkBindModule::class],
)
interface TestDownloadWorkBindModule {

    @Binds
    @Singleton
    fun bindsDownloadWorkManager(impl: TestDownloadWorkManager): DownloadWorkManager
}
