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

package me.proton.core.drive.feature.flag.data.di

import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.feature.flag.data.manager.FeatureFlagWorkManagerImpl
import me.proton.core.drive.feature.flag.domain.manager.FeatureFlagWorkManager
import me.proton.core.presentation.app.AppLifecycleProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class FeatureFlagModule {

    @Singleton
    @Provides
    fun provideWorkManager(
        workManager: WorkManager,
        configurationProvider: ConfigurationProvider,
        appLifecycleProvider: AppLifecycleProvider,
    ): FeatureFlagWorkManager =
        FeatureFlagWorkManagerImpl(
            workManager = workManager,
            configurationProvider = configurationProvider,
            appLifecycleProvider = appLifecycleProvider,
            coroutineContext = Job() + Dispatchers.IO,
        )
}
