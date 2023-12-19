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

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import me.proton.android.drive.ui.rules.NetworkSimulator
import me.proton.core.drive.backup.data.di.BackupModule
import me.proton.core.drive.backup.data.manager.BackupConnectivityManagerImpl
import me.proton.core.drive.backup.domain.manager.BackupConnectivityManager
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [BackupModule::class]
)
class TestBackupModule {
    @Provides
    @Singleton
    fun provideBackupConnectivityManager(impl: BackupConnectivityManagerImpl): BackupConnectivityManager =
        object : BackupConnectivityManager {
            override val connectivity: Flow<BackupConnectivityManager.Connectivity>
                get() = NetworkSimulator.connectivity ?: impl.connectivity
        }

    @Provides
    @Singleton
    fun provideBackupConnectivityManagerImpl(@ApplicationContext appContext: Context): BackupConnectivityManagerImpl =
        BackupConnectivityManagerImpl(
            appContext = appContext,
            coroutineContext = Dispatchers.Main + Job()
        )
}