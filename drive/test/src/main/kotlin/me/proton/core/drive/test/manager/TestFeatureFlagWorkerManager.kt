/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.test.manager

import android.content.Context
import androidx.work.ListenableWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.FeatureFlagWorkerManager
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.featureflag.domain.entity.FeatureId
import me.proton.core.featureflag.domain.repository.FeatureFlagLocalDataSource
import me.proton.core.featureflag.domain.repository.FeatureFlagRemoteDataSource
import me.proton.core.featureflag.domain.usecase.FetchFeatureIdsRemote
import me.proton.core.featureflag.domain.usecase.FetchUnleashTogglesRemote
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.isRetryable
import javax.inject.Inject

class TestFeatureFlagWorkerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : FeatureFlagWorkerManager {

    // avoid cyclic dependencies between FeatureFlagWorkerManager and FeatureFlagRepository
    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TestEntryPoint::class.java
        )
    }

    override fun enqueueOneTime(userId: UserId?) {
        runBlocking {
            entryPoint.fetchUnleashTogglesRemote(userId)
        }
    }

    override fun enqueuePeriodic(userId: UserId?, immediately: Boolean) {
        enqueueOneTime(userId)
    }

    override fun prefetch(userId: UserId?, featureIds: Set<FeatureId>) {
        runBlocking {
            entryPoint.fetchFeatureIdsRemote(userId, featureIds.toSet())
        }
    }

    override fun update(featureFlag: FeatureFlag) {
        runBlocking {
            val userId = featureFlag.userId
            val featureId = featureFlag.featureId
            val isEnabled = featureFlag.value
            runCatching {
                entryPoint.remoteDataSource.update(userId, featureId, isEnabled)
                ListenableWorker.Result.success()
            }.getOrElse { error ->
                if (error is ApiException && error.isRetryable()) {
                    ListenableWorker.Result.retry()
                } else {
                    entryPoint.localDataSource.updateValue(userId, featureId, isEnabled.not())
                }
            }
        }
    }

    override fun cancel(userId: UserId?) {
        // do nothing
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TestEntryPoint {
        val  fetchFeatureIdsRemote: FetchFeatureIdsRemote
        val fetchUnleashTogglesRemote: FetchUnleashTogglesRemote
        val remoteDataSource: FeatureFlagRemoteDataSource
        val localDataSource: FeatureFlagLocalDataSource
    }
}
