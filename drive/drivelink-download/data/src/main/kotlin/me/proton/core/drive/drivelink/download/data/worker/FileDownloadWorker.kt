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

import android.annotation.SuppressLint
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkContinuation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.download.data.extension.logTag
import me.proton.core.drive.drivelink.download.data.extension.uniqueWorkName
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_FILE_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_FILE_TAGS
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_RETRYABLE
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_REVISION_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_VOLUME_ID
import me.proton.core.drive.drivelink.download.domain.usecase.SetDownloadingAndGetRevision
import me.proton.core.drive.file.base.domain.entity.Revision
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.usecase.SetDownloadState
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.worker.data.LimitedRetryCoroutineWorker
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.isRetryable
import me.proton.core.util.kotlin.CoreLogger
import java.util.concurrent.TimeUnit

@HiltWorker
class FileDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val workManager: WorkManager,
    private val setDownloadingAndGetRevision: SetDownloadingAndGetRevision,
    private val setDownloadState: SetDownloadState,
    private val configurationProvider: ConfigurationProvider,
    canRun: CanRun,
    run: Run,
    done: Done,
) : LimitedRetryCoroutineWorker(appContext, workerParams, canRun, run, done) {
    override val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val volumeId = requireNotNull(inputData.getString(KEY_VOLUME_ID))
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)))
    private val fileId = FileId(shareId, requireNotNull(inputData.getString(KEY_FILE_ID)))
    private val revisionId = requireNotNull(inputData.getString(KEY_REVISION_ID))
    private val isRetryable = inputData.getBoolean(KEY_RETRYABLE, false)
    private val fileTags = inputData.getStringArray(KEY_FILE_TAGS).orEmpty().toList()
    override val logTag = fileId.logTag

    override suspend fun doLimitedRetryWork(): Result =
        try {
            CoreLogger.d(logTag, "Started downloading file with revision ${revisionId.logId()}")
            downloadBlocks(setDownloadingAndGetRevision(fileId, revisionId).getOrThrow())
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            if (isRetryable && e is ApiException && e.isRetryable()) {
                e.log(
                    tag = logTag,
                    message = "Downloading file with revision ${revisionId.logId()} failed but will retry"
                )
                Result.retry()
            } else {
                e.log(
                    tag = logTag,
                    message = "Downloading file with revision ${revisionId.logId()} failed"
                )
                setDownloadState(fileId, DownloadState.Error)
                Result.failure()
            }
        }

    @SuppressLint("EnqueueWork")
    private fun downloadBlocks(revision: Revision): Result {
        revision.blocks.map { block ->
            BlockDownloadWorker.getWorkRequest(
                userId = userId,
                volumeId = volumeId,
                revisionId = revisionId,
                block = block,
                isRetryable = isRetryable,
                tags = fileTags,
            )
        }
            .also { blocks ->
                if (blocks.isEmpty()) {
                    workManager.enqueue(verifyDownload())
                    return Result.success()
                }
            }
            .chunked(configurationProvider.downloadsInParallel)
            .fold<List<OneTimeWorkRequest>, List<WorkContinuation>>(emptyList()) { continuations, requests ->
                if (continuations.isEmpty()) {
                    requests.map { request ->
                        workManager.beginWith(request)
                    }
                } else {
                    require(requests.size <= continuations.size) {
                        "Requests size must not be bigger than continuations size"
                    }
                    val latest = requests.zip(continuations).map { (request, continuation) ->
                        continuation.then(request)
                    }
                    if (requests.size == continuations.size) latest
                    else latest + continuations.subList(requests.size, continuations.size)
                }
            }.also { continuations ->
                WorkContinuation.combine(continuations)
                    .then(verifyDownload())
                    .enqueue()
            }

        return Result.success()
    }

    private fun verifyDownload() = FileDownloadVerifyWorker.getWorkRequest(
        userId = userId,
        volumeId = volumeId,
        fileId = fileId,
        revisionId = revisionId,
        retryable = isRetryable,
        fileTags = fileTags,
    )

    companion object {

        fun getWorkRequest(
            userId: UserId,
            volumeId: VolumeId,
            fileId: FileId,
            revisionId: String,
            isRetryable: Boolean,
            fileTags: List<String> = emptyList(),
            tags: Collection<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(FileDownloadWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, userId.id)
                        .putString(KEY_VOLUME_ID, volumeId.id)
                        .putString(KEY_SHARE_ID, fileId.shareId.id)
                        .putString(KEY_FILE_ID, fileId.id)
                        .putString(KEY_REVISION_ID, revisionId)
                        .putBoolean(KEY_RETRYABLE, isRetryable)
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


        fun getWorkRequest(
            driveLink: DriveLink.File,
            retryable: Boolean,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            getWorkRequest(
                userId = driveLink.userId,
                volumeId = driveLink.volumeId,
                fileId = driveLink.id,
                revisionId = driveLink.activeRevisionId,
                isRetryable = retryable,
                fileTags = listOf(driveLink.uniqueWorkName),
                tags = tags,
            )
    }
}
