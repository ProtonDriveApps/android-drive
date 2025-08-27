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
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.drivelink.download.data.extension.getSizeData
import me.proton.core.drive.drivelink.download.data.extension.setSize
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_BLOCK_HASH
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_BLOCK_INDEX
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_BLOCK_URL
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_FILE_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_REVISION_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_VOLUME_ID
import me.proton.core.drive.drivelink.download.domain.extension.post
import me.proton.core.drive.drivelink.download.domain.manager.DownloadErrorManager
import me.proton.core.drive.drivelink.download.domain.usecase.DownloadBlock
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.file.base.domain.exception.CancelledException
import me.proton.core.drive.file.base.domain.extension.verifyOrDelete
import me.proton.core.drive.file.base.domain.usecase.GetBlockFile
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import java.util.concurrent.TimeUnit

@HiltWorker
class BlockDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getBlockFile: GetBlockFile,
    private val downloadBlock: DownloadBlock,
    private val downloadErrorManager: DownloadErrorManager,
) : CoroutineWorker(appContext, workerParams) {
    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val volumeId = VolumeId(requireNotNull(inputData.getString(KEY_VOLUME_ID)))
    private val revisionId = requireNotNull(inputData.getString(KEY_REVISION_ID))
    private val index = inputData.getLong(KEY_BLOCK_INDEX, -1)
    private val url = requireNotNull(inputData.getString(KEY_BLOCK_URL))
    private val hash = requireNotNull(inputData.getString(KEY_BLOCK_HASH))
    private val logTag = "${LogTag.DOWNLOAD}.${revisionId.logId()}.$index"
    private val fileId: FileId? = inputData.getString(KEY_SHARE_ID)?.let { shareIdString ->
        inputData.getString(KEY_FILE_ID)?.let { fileIdString ->
            FileId(ShareId(userId, shareIdString), fileIdString)
        }
    }

    override suspend fun doWork(): Result = coroutineScope {
        CoreLogger.d(logTag, "Started downloading block")
        val block = Block(index, url, hash, "")
        getBlockFile(userId, volumeId, revisionId, block)?.run {
            if (verifyOrDelete(block.hashSha256)) {
                // skip this block
                CoreLogger.d(logTag, "Skipping this block as it's already downloaded")
                return@coroutineScope Result.success(getSizeData(length()))
            }
        }
        val progress = MutableStateFlow(0L)
        val job = progress.onEach { size -> setSize(size) }.launchIn(this)
        downloadBlock(
            userId = userId,
            volumeId = volumeId,
            revisionId = revisionId,
            block = block,
            downloadingProgress = progress,
            isCancelled = ::isStopped,
        )
            .onSuccess { file ->
                CoreLogger.d(logTag, "Download successful at ${file.path}")
                job.cancelAndJoin()
                return@coroutineScope Result.success(getSizeData(file.length()))
            }
            .onFailure { error ->
                error.log(
                    tag = logTag,
                    message = if (error.cause is CancelledException) {
                        "Download was cancelled, file will be deleted"
                    } else {
                        "Downloading block failed"
                    }
                )
                fileId?.let {
                    downloadErrorManager.post(fileId, error, error.cause is CancelledException)
                }
            }
        job.cancelAndJoin()
        Result.success(getSizeData(0L))
    }

    companion object {
        fun getWorkRequest(
            volumeId: String,
            fileId: FileId,
            revisionId: String,
            block: Block,
            isRetryable: Boolean,
            tags: Collection<String> = emptyList(),
        ): OneTimeWorkRequest = OneTimeWorkRequest.Builder(BlockDownloadWorker::class.java)
            .setConstraints(
                Constraints.Builder().apply {
                    if (isRetryable) {
                        setRequiredNetworkType(NetworkType.CONNECTED)
                    }
                }.build()
            )
            .setInputData(
                Data.Builder()
                    .putString(KEY_USER_ID, fileId.userId.id)
                    .putString(KEY_VOLUME_ID, volumeId)
                    .putString(KEY_SHARE_ID, fileId.shareId.id)
                    .putString(KEY_FILE_ID, fileId.id)
                    .putString(KEY_REVISION_ID, revisionId)
                    .putLong(KEY_BLOCK_INDEX, block.index)
                    .putString(KEY_BLOCK_URL, block.url)
                    .putString(KEY_BLOCK_HASH, block.hashSha256)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTags(listOf(fileId.userId.id) + tags)
            .build()
    }
}
