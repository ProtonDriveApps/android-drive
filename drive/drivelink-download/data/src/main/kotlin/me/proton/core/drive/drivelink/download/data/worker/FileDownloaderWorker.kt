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

package me.proton.core.drive.drivelink.download.data.worker

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.drivelink.download.domain.entity.DownloadFileLink
import me.proton.core.drive.drivelink.download.domain.repository.DownloadFileRepository
import me.proton.core.util.kotlin.CoreLogger
import kotlin.math.pow
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HiltWorker
class FileDownloaderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadFileRepository: DownloadFileRepository,
) : CoroutineWorker(appContext, workerParams) {
    val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))

    override suspend fun doWork(): Result {
        try {
            CoreLogger.d(LogTag.DOWNLOAD, "FileDownloaderWorker started")
            while (true) {
                if ((downloadFileRepository.getCountFlow(userId).firstOrNull() ?: 0) > 0) {
                    processRetryableDownloads()
                    delay(1.seconds)
                } else {
                    break
                }
            }
        } catch (e: CancellationException) {
            CoreLogger.d(LogTag.DOWNLOAD, "FileDownloaderWorker cancelled")
            throw e
        }
        CoreLogger.d(LogTag.DOWNLOAD, "FileDownloaderWorker stopped")
        return Result.success()
    }

    private suspend fun processRetryableDownloads() {
        downloadFileRepository.getAllWithState(userId, DownloadFileLink.State.FAILED)
            .forEach { downloadFileLink ->
                if (downloadFileLink.canRetry) {
                    downloadFileRepository.resetStateAndIncreaseRetries(
                        downloadFileLink.id,
                        DownloadFileLink.State.IDLE,
                    )
                }
            }
    }

    private val DownloadFileLink.canRetry: Boolean get() =
        (System.currentTimeMillis() - (lastRunTimestamp ?: 0)) >
                (2.0.pow(numberOfRetries) * retryInterval)
                    .toLong()
                    .coerceAtMost(maxRetryInterval)

    companion object {
        private val retryInterval = 1.seconds.inWholeMilliseconds
        private val maxRetryInterval = 2.minutes.inWholeMilliseconds
        const val UNIQUE_WORK_NAME = "FileDownloadWorker"
        fun getWorkRequest(userId: UserId): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(FileDownloaderWorker::class.java)
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
                .build()
    }
}
