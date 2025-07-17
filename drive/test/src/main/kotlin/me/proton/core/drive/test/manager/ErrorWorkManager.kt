/*
 * Copyright (c) 2025 Proton AG. 
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

import android.annotation.SuppressLint
import android.app.PendingIntent
import androidx.lifecycle.LiveData
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkContinuation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.WorkRequest
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@SuppressLint("RestrictedApi")
object ErrorWorkManager : WorkManager() {
    private const val message =
        "WorkManager should not be injected for test, create a Test*WorkManager implementation that only uses use cases"
    
    override fun getConfiguration(): Configuration {
        error(message)
    }

    override fun enqueue(requests: MutableList<out WorkRequest>): Operation {
        error(message)
    }

    override fun beginWith(work: MutableList<OneTimeWorkRequest>): WorkContinuation {
        error(message)
    }

    override fun beginUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: ExistingWorkPolicy,
        work: MutableList<OneTimeWorkRequest>
    ): WorkContinuation {
        error(message)
    }

    override fun enqueueUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: ExistingWorkPolicy,
        work: MutableList<OneTimeWorkRequest>
    ): Operation {
        error(message)
    }

    override fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        periodicWork: PeriodicWorkRequest
    ): Operation {
        error(message)
    }

    override fun cancelWorkById(id: UUID): Operation {
        error(message)
    }

    override fun cancelAllWorkByTag(tag: String): Operation {
        error(message)
    }

    override fun cancelUniqueWork(uniqueWorkName: String): Operation {
        error(message)
    }

    override fun cancelAllWork(): Operation {
        error(message)
    }

    override fun createCancelPendingIntent(id: UUID): PendingIntent {
        error(message)
    }

    override fun pruneWork(): Operation {
        error(message)
    }

    override fun getLastCancelAllTimeMillisLiveData(): LiveData<Long> {
        error(message)
    }

    override fun getLastCancelAllTimeMillis(): ListenableFuture<Long> {
        error(message)
    }

    override fun getWorkInfoByIdLiveData(id: UUID): LiveData<WorkInfo> {
        error(message)
    }

    override fun getWorkInfoByIdFlow(id: UUID): Flow<WorkInfo> {
        error(message)
    }

    override fun getWorkInfoById(id: UUID): ListenableFuture<WorkInfo> {
        error(message)
    }

    override fun getWorkInfosByTagLiveData(tag: String): LiveData<MutableList<WorkInfo>> {
        error(message)
    }

    override fun getWorkInfosByTagFlow(tag: String): Flow<MutableList<WorkInfo>> {
        error(message)
    }

    override fun getWorkInfosByTag(tag: String): ListenableFuture<MutableList<WorkInfo>> {
        error(message)
    }

    override fun getWorkInfosForUniqueWorkLiveData(uniqueWorkName: String): LiveData<MutableList<WorkInfo>> {
        error(message)
    }

    override fun getWorkInfosForUniqueWorkFlow(uniqueWorkName: String): Flow<MutableList<WorkInfo>> {
        error(message)
    }

    override fun getWorkInfosForUniqueWork(uniqueWorkName: String): ListenableFuture<MutableList<WorkInfo>> {
        error(message)
    }

    override fun getWorkInfosLiveData(workQuery: WorkQuery): LiveData<MutableList<WorkInfo>> {
        error(message)
    }

    override fun getWorkInfosFlow(workQuery: WorkQuery): Flow<MutableList<WorkInfo>> {
        error(message)
    }

    override fun getWorkInfos(workQuery: WorkQuery): ListenableFuture<MutableList<WorkInfo>> {
        error(message)
    }

    override fun updateWork(request: WorkRequest): ListenableFuture<UpdateResult> {
        error(message)
    }
}
