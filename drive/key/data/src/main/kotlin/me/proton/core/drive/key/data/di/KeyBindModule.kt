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
package me.proton.core.drive.key.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.key.data.factory.ContentKeyFactoryImpl
import me.proton.core.drive.key.data.repository.KeyRepositoryImpl
import me.proton.core.drive.key.data.repository.StalePublicAddressKeyRepositoryImpl
import me.proton.core.drive.key.domain.factory.ContentKeyFactory
import me.proton.core.drive.key.domain.repository.KeyRepository
import me.proton.core.drive.key.domain.repository.StalePublicAddressKeyRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface KeyBindModule {

    @Binds
    @Singleton
    fun bindsKeyRepositoryImpl(impl: KeyRepositoryImpl): KeyRepository

    @Binds
    @Singleton
    fun bindsFactoryImpl(impl: ContentKeyFactoryImpl): ContentKeyFactory

    @Binds
    @Singleton
    fun bindsStalePublicAddressKeRepositoryImpl(
        impl: StalePublicAddressKeyRepositoryImpl
    ): StalePublicAddressKeyRepository
}
