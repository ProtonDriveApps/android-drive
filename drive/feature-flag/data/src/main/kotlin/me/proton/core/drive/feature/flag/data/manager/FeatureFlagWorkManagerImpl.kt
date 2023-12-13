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

package me.proton.core.drive.feature.flag.data.manager

import androidx.work.ExistingWorkPolicy
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.feature.flag.data.worker.FeatureFlagRefreshWorker
import me.proton.core.drive.feature.flag.domain.manager.FeatureFlagWorkManager
import me.proton.core.presentation.app.AppLifecycleProvider
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FeatureFlagWorkManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val configurationProvider: ConfigurationProvider,
    private val appLifecycleProvider: AppLifecycleProvider,
    private val coroutineContext: CoroutineContext,
) : FeatureFlagWorkManager {

    private var enqueueJobs: MutableMap<UserId, Job> = mutableMapOf()

    override suspend fun start(userId: UserId) {
        enqueueJobs.remove(userId)?.cancel()
        enqueueJobs[userId] = appLifecycleProvider
            .state
            .onEach { state ->
                when (state) {
                    AppLifecycleProvider.State.Foreground -> enqueue(userId, 0.seconds).await()
                    AppLifecycleProvider.State.Background -> cancel(userId)
                }
            }
            .launchIn(CoroutineScope(coroutineContext))
    }

    override suspend fun stop(userId: UserId) {
        enqueueJobs.remove(userId)?.cancel()
        cancel(userId)
    }

    override suspend fun enqueue(userId: UserId) {
        enqueue(userId, configurationProvider.featureFlagFreshDuration)
    }

    private fun enqueue(userId: UserId, initialDelay: Duration): Operation =
        workManager
            .enqueueUniqueWork(
                uniqueWorkName(userId),
                ExistingWorkPolicy.REPLACE,
                FeatureFlagRefreshWorker.getWorkRequest(
                    userId = userId,
                    initialDelay = initialDelay,
                    tags = listOf(refreshWorkerTag(userId)),
                ),
            )

    override suspend fun cancel(userId: UserId) {
        workManager.cancelUniqueWork(uniqueWorkName(userId)).await()
    }

    private fun uniqueWorkName(userId: UserId): String = "${this::class.java.simpleName}-user-id-${userId.id}"

    private fun refreshWorkerTag(userId: UserId): String =
        "${FeatureFlagRefreshWorker::class.java.simpleName}-user-id-${userId.id}"
}
