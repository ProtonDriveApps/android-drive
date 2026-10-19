/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.initializer

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.proton.android.drive.provider.AppProtonSdkClientProvider
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.drive.base.domain.coroutines.ApplicationScope
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.drive.sdk.ProtonDriveSdk

class ProtonDriveSdkInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SdkInitializerEntryPoint::class.java
        ).run {
            // Loading the sdk first and the app don't crash
            applicationScope.launch(Dispatchers.IO) {
                ProtonDriveSdk
            }
            accountManager.observe(appLifecycleProvider.lifecycle, Lifecycle.State.RESUMED)
                .onAccountRemoved { account ->
                    appProtonSdkClientProvider.remove(account.userId)
                }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        WorkManagerInitializer::class.java,
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SdkInitializerEntryPoint {
    val configurationProvider: ConfigurationProvider
    val accountManager: AccountManager
    val appLifecycleProvider: AppLifecycleProvider
    val appProtonSdkClientProvider: AppProtonSdkClientProvider
    @get:ApplicationScope
    val applicationScope: CoroutineScope
}
