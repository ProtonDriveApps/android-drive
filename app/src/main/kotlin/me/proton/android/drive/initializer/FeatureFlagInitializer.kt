/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and Proton Pass.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Pass. If not, see <https://www.gnu.org/licenses/>.
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.BuildConfig
import me.proton.android.drive.extension.log
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.feature.flag.data.extension.toFeatureFlag
import me.proton.core.drive.feature.flag.domain.usecase.GetObservableFeatureIds
import me.proton.core.drive.feature.flag.domain.usecase.HandleFeatureFlags
import me.proton.core.drive.feature.flag.domain.usecase.StartFeatureFlagRefreshPolling
import me.proton.core.drive.feature.flag.domain.usecase.StopFeatureFlagRefreshPolling
import me.proton.core.featureflag.data.FeatureFlagRefreshStarter
import me.proton.core.featureflag.domain.repository.FeatureFlagRepository
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.util.kotlin.takeIfNotEmpty

class FeatureFlagInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        with(
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                FeatureFlagInitializerEntryPoint::class.java
            )
        ) {
            featureFlagRefreshStarter().start(BuildConfig.DEBUG)
            val jobs: MutableMap<UserId, Job?> = mutableMapOf()
            accountManager.observe(appLifecycleProvider.lifecycle, Lifecycle.State.STARTED)
                .onAccountReady { account ->
                    startFeatureFlagRefreshPolling(account.userId)
                        .onFailure { error ->
                            error.log(LogTag.FEATURE_FLAG)
                        }
                    jobs[account.userId] = notifyFeatureFlagChange(account.userId)
                }
                .onAccountRemoved { account ->
                    jobs.remove(account.userId)?.cancel()
                    stopFeatureFlagRefreshPolling(account.userId)
                        .onFailure { error ->
                            error.log(LogTag.FEATURE_FLAG)
                        }
                }
        }
    }

    private fun FeatureFlagInitializerEntryPoint.notifyFeatureFlagChange(userId: UserId): Job? =
        getObservableFeatureIds()
            .takeIfNotEmpty()
            ?.let { featureIds ->
                featureFlagRepository.observe(userId, featureIds)
                    .distinctUntilChanged()
                    .onEach { featureFlags ->
                        handleFeatureFlags(
                            featureFlags.map { featureFlag -> featureFlag.toFeatureFlag(userId) }
                        )
                    }
                    .launchIn(appLifecycleProvider.lifecycle.coroutineScope)
            }

    override fun dependencies(): List<Class<out Initializer<*>?>> = listOf(
        WorkManagerInitializer::class.java
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FeatureFlagInitializerEntryPoint {
        fun featureFlagRefreshStarter(): FeatureFlagRefreshStarter
        val accountManager: AccountManager
        val appLifecycleProvider: AppLifecycleProvider
        val startFeatureFlagRefreshPolling: StartFeatureFlagRefreshPolling
        val stopFeatureFlagRefreshPolling: StopFeatureFlagRefreshPolling
        val featureFlagRepository: FeatureFlagRepository
        val handleFeatureFlags: HandleFeatureFlags
        val getObservableFeatureIds: GetObservableFeatureIds
    }
}
