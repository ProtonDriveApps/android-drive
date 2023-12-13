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

package me.proton.android.drive.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.android.drive.usecase.CleanUpAccount
import me.proton.core.domain.entity.UserId
import java.util.concurrent.TimeUnit

@HiltWorker
class AccountRemovedCleanUpWorker @AssistedInject constructor(
    private val cleanUpAccount: CleanUpAccount,
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val userId = UserId(inputData.getString(KEY_USER_ID) ?: "")

    override suspend fun doWork(): Result {
        cleanUpAccount(userId)
        return Result.success()
    }

    companion object {
        private const val KEY_USER_ID = "USER_ID"
        private const val UNIQUE_WORK_NAME_FORMAT = "AccountRemovedCleanUp_%s"

        fun cleanUp(workManager: WorkManager, userId: UserId) {
            workManager.beginUniqueWork(
                UNIQUE_WORK_NAME_FORMAT.format(userId.id),
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequest.Builder(AccountRemovedCleanUpWorker::class.java)
                    .setInputData(
                        Data.Builder().putString(KEY_USER_ID, userId.id).build()
                    ).setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .build(),
            ).enqueue()
        }

        fun cancel(workManager: WorkManager, userId: UserId) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME_FORMAT.format(userId.id))
        }
    }
}
