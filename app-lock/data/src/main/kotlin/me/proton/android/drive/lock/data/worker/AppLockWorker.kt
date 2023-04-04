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

package me.proton.android.drive.lock.data.worker

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.android.drive.lock.domain.usecase.LockApp
import me.proton.core.drive.base.data.workmanager.addTags
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

@HiltWorker
@TargetApi(Build.VERSION_CODES.O)
class AppLockWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val lockApp: LockApp,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        lockApp()
        return Result.success()
    }

    companion object {
        fun getWorkRequest(
            runAfter: Duration,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(AppLockWorker::class.java)
                .setInitialDelay(runAfter.inWholeSeconds, TimeUnit.SECONDS)
                .addTags(tags)
                .build()
    }
}
