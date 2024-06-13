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

package me.proton.core.drive.drivelink.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.drivelink.data.db.DriveLinkDatabase
import me.proton.core.drive.drivelink.data.repository.DriveLinkRepositoryImpl
import me.proton.core.drive.drivelink.data.usecase.UpdateDriveLinkDisplayNameImpl
import me.proton.core.drive.drivelink.domain.repository.DriveLinkRepository
import me.proton.core.drive.drivelink.domain.usecase.UpdateDriveLinkDisplayName

@Module
@InstallIn(SingletonComponent::class)
interface DriveLinkBindsModule {
    @Binds
    fun bindsDriveLinkRepository(repository: DriveLinkRepositoryImpl): DriveLinkRepository
    @Binds
    fun bindsUpdateDriveLinkDisplayName(impl: UpdateDriveLinkDisplayNameImpl): UpdateDriveLinkDisplayName
}

@Module
@InstallIn(SingletonComponent::class)
object DriveLinkModule {
    @Provides
    fun provideDriveLinkDao(database: DriveLinkDatabase) = database.driveLinkDao
}

