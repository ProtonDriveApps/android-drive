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

package me.proton.android.drive.initializer

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.extension.log
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag.ENTITLEMENT
import me.proton.core.drive.entitlement.domain.usecase.RefreshEntitlements
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveDynamicEntitlementConfiguration
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.user.domain.usecase.ObserveUser

class EntitlementInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        with(
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                EntitlementInitializerEntryPoint::class.java
            )
        ) {
            val jobs: MutableMap<UserId, Job?> = mutableMapOf()
            accountManager.observe(appLifecycleProvider.lifecycle, Lifecycle.State.STARTED)
                .onAccountReady { account ->
                    jobs[account.userId] =
                        observeUser(account.userId)
                            .filterNotNull()
                            .map { it.subscribed }
                            .distinctUntilChanged()
                            .onEach {
                                if (getFeatureFlag(driveDynamicEntitlementConfiguration(account.userId)).on) {
                                    refreshEntitlements(account.userId).onFailure { error ->
                                        error.log(ENTITLEMENT, "Cannot refresh entitlements")
                                    }
                                }
                            }.launchIn(appLifecycleProvider.lifecycle.coroutineScope)
                }
                .onAccountRemoved { account ->
                    jobs.remove(account.userId)?.cancel()
                }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>> = listOf(
        WorkManagerInitializer::class.java
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EntitlementInitializerEntryPoint {
        val accountManager: AccountManager
        val appLifecycleProvider: AppLifecycleProvider
        val observeUser: ObserveUser
        val refreshEntitlements: RefreshEntitlements
        val getFeatureFlag: GetFeatureFlag
    }
}
