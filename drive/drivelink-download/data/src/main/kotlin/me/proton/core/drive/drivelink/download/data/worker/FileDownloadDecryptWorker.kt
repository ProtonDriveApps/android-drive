/*
 * Copyright (c) 2021-2023 Proton AG.
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
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.download.data.extension.logTag
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_FILE_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_FILE_TAGS
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_RETRYABLE
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_REVISION_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_VOLUME_ID
import me.proton.core.drive.drivelink.download.domain.extension.post
import me.proton.core.drive.drivelink.download.domain.manager.DownloadErrorManager
import me.proton.core.drive.drivelink.download.domain.usecase.DecryptDownloadedBlocks
import me.proton.core.drive.drivelink.download.domain.usecase.DownloadMetricsNotifier
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class FileDownloadDecryptWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val workManager: WorkManager,
    private val downloadErrorManager: DownloadErrorManager,
    private val downloadMetricsNotifier: DownloadMetricsNotifier,
    private val getDriveLink: GetDriveLink,
    private val decryptDownloadedBlocks: DecryptDownloadedBlocks,
) : CoroutineWorker(appContext, workerParams) {
    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val volumeId = VolumeId(requireNotNull(inputData.getString(KEY_VOLUME_ID)))
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)))
    private val fileId = FileId(shareId, requireNotNull(inputData.getString(KEY_FILE_ID)))
    private val revisionId = requireNotNull(inputData.getString(KEY_REVISION_ID))
    private val retryable = inputData.getBoolean(KEY_RETRYABLE, false)
    private val fileTags = inputData.getStringArray(KEY_FILE_TAGS).orEmpty().toList()
    private val logTag = fileId.logTag

    override suspend fun doWork(): Result {
        CoreLogger.d(logTag, "Started decrypting downloaded blocks")
        val driveLink = getDriveLink(fileId).toResult().getOrThrow()

        return decryptDownloadedBlocks(driveLink).fold(onSuccess = {
            CoreLogger.i(
                logTag,
                "File ${driveLink.id.id.logId()} was successfully decrypted!"
            )
            Result.success()
        }, onFailure = { error ->
            CoreLogger.e(
                logTag,
                error,
                "There was an error decrypting file ${driveLink.id.id.logId()}"
            )
            if (retryable) {
                workManager.enqueue(
                    FileDownloadWorker.getWorkRequest(
                        userId = userId,
                        volumeId = volumeId,
                        fileId = fileId,
                        revisionId = revisionId,
                        isRetryable = retryable,
                        fileTags = fileTags,
                    )
                )
            }
            downloadErrorManager.post(
                fileId,
                error,
                isCancelledByUser = error is CancellationException
            )
            downloadMetricsNotifier(fileId, false, error)
            error.log(logTag)
            if (error.isRetryable) {
                Result.retry()
            } else {
                if (retryable) {
                    workManager.enqueue(
                        FileDownloadWorker.getWorkRequest(
                            userId = userId,
                            volumeId = volumeId,
                            fileId = fileId,
                            revisionId = revisionId,
                            isRetryable = retryable,
                            fileTags = fileTags,
                        )
                    )
                }
                Result.failure()
            }
        })
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            volumeId: String,
            fileId: FileId,
            revisionId: String,
            retryable: Boolean,
            fileTags: List<String> = emptyList(),
            tags: Collection<String> = emptyList(),
        ) = OneTimeWorkRequest.Builder(FileDownloadDecryptWorker::class.java)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                Data.Builder()
                    .putString(KEY_USER_ID, userId.id)
                    .putString(KEY_VOLUME_ID, volumeId)
                    .putString(KEY_SHARE_ID, fileId.shareId.id)
                    .putString(KEY_FILE_ID, fileId.id)
                    .putString(KEY_REVISION_ID, revisionId)
                    .putBoolean(KEY_RETRYABLE, retryable)
                    .putStringArray(KEY_FILE_TAGS, fileTags.toTypedArray())
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTags(listOf(userId.id) + fileTags + tags)
            .build()
    }
}
