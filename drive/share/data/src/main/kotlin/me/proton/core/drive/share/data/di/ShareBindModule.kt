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
package me.proton.core.drive.share.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.share.data.repository.MigrationKeyPacketRepositoryImpl
import me.proton.core.drive.share.data.repository.ShareRepositoryImpl
import me.proton.core.drive.share.domain.repository.MigrationKeyPacketRepository
import me.proton.core.drive.share.domain.repository.ShareRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ShareBindModule {

    @Binds
    @Singleton
    fun bindsRepositoryImpl(impl: ShareRepositoryImpl): ShareRepository

    @Binds
    @Singleton
    fun bindsMigrationKeyPacketRepositoryImpl(impl: MigrationKeyPacketRepositoryImpl): MigrationKeyPacketRepository
}
