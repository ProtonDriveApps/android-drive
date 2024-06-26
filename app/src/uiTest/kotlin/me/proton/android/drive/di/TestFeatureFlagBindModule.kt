/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import me.proton.android.drive.repository.TestFeatureFlagRepositoryImpl
import me.proton.core.drive.feature.flag.data.di.FeatureFlagBindModule
import me.proton.core.drive.feature.flag.data.repository.FeatureFlagRepositoryImpl
import me.proton.core.drive.feature.flag.domain.repository.FeatureFlagRepository
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [FeatureFlagBindModule::class]
)
object TestFeatureFlagBindModule {

    @Provides
    @Singleton
    fun provideFeatureFlagRepository(
        impl : FeatureFlagRepositoryImpl
    ): FeatureFlagRepository = TestFeatureFlagRepositoryImpl(impl)
}
