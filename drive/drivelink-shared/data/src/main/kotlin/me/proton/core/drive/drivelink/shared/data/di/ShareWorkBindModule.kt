/*
 * Copyright (c) 2021-2024 Proton AG.
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
 */package me.proton.core.drive.drivelink.shared.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.drivelink.shared.data.manager.ShareWorkManagerImpl
import me.proton.core.drive.drivelink.shared.domain.manager.ShareWorkManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ShareWorkBindModule {

    @Binds
    @Singleton
    fun bindsShareWorkManagerImpl(impl: ShareWorkManagerImpl): ShareWorkManager
}
