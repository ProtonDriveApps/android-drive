/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.feature.flag.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.entity.LoggerLevel.WARNING
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.manager.FeatureFlagWorkManager
import me.proton.core.drive.feature.flag.domain.usecase.RefreshFeatureFlags
import me.proton.core.util.kotlin.CoreLogger
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

@HiltWorker
class FeatureFlagRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val refreshFeatureFlags: RefreshFeatureFlags,
    private val featureFlagWorkManager: FeatureFlagWorkManager,
) : CoroutineWorker(appContext, workerParams) {
    val userId = UserId(
        requireNotNull(inputData.getString(KEY_USER_ID)) { "User id is required" }
    )

    override suspend fun doWork(): Result = coRunCatching {
        CoreLogger.d(LogTag.FEATURE_FLAG, "Feature flag refresh worker")
        refreshFeatureFlags(userId).getOrThrow()
    }.recoverCatching { error ->
        if (error is IllegalStateException) {
            CoreLogger.d(LogTag.FEATURE_FLAG, error, "Ignoring ${error.message}")
        } else {
            throw error
        }
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { error ->
            if (error.isRetryable) {
                error.log(LogTag.FEATURE_FLAG, "Cannot refresh feature flag, will retry", WARNING)
                Result.retry()
            } else {
                error.log(LogTag.FEATURE_FLAG, "Cannot refresh feature flag")
                Result.failure()
            }
        }
    ).also { result ->
        when (result) {
            is Result.Retry -> Unit
            else -> featureFlagWorkManager.enqueue(userId)
        }
    }

    companion object {
        private const val KEY_USER_ID = "key.userId"

        fun getWorkRequest(
            userId: UserId,
            initialDelay: Duration,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest = OneTimeWorkRequest.Builder(FeatureFlagRefreshWorker::class.java)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                Data.Builder()
                    .putString(KEY_USER_ID, userId.id)
                    .build()
            )
            .setInitialDelay(initialDelay.inWholeSeconds, TimeUnit.SECONDS)
            .addTags(listOf(userId.id) + tags)
            .build()
    }
}
