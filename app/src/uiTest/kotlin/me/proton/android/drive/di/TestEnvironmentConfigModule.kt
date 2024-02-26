/*
 * Copyright (c) 2023 Proton AG.
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

import androidx.test.platform.app.InstrumentationRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import me.proton.core.configuration.EnvironmentConfiguration
import me.proton.core.configuration.dagger.ContentResolverEnvironmentConfigModule
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ContentResolverEnvironmentConfigModule::class]
)
object TestEnvironmentConfigModule {
    @Provides
    @Singleton
    fun provideEnvironmentConfiguration(): EnvironmentConfiguration =
        EnvironmentConfiguration(::instrumentationArgumentStringProvider)

    private fun instrumentationArgumentStringProvider(key: String): String? =
        InstrumentationRegistry.getArguments().getString(key)
}
