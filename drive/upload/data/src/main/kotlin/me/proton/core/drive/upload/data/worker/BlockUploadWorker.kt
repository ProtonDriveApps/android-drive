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
package me.proton.core.drive.upload.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.api.ProtonApiCode.NOT_EXISTS
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.data.workmanager.onProtonHttpException
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadBlock
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.UpdateToken
import me.proton.core.drive.upload.data.extension.getSizeData
import me.proton.core.drive.upload.data.extension.isRetryable
import me.proton.core.drive.upload.data.extension.setSize
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_BLOCK_INDEX
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_BLOCK_TOKEN
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_BLOCK_URL
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.upload.domain.usecase.GetBlocksUploadUrl
import me.proton.core.drive.upload.domain.usecase.UploadBlock
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run

@HiltWorker
@OptIn(ExperimentalCoroutinesApi::class)
class BlockUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    workManager: WorkManager,
    broadcastMessages: BroadcastMessages,
    getUploadFileLink: GetUploadFileLink,
    private val uploadBlock: UploadBlock,
    private val getUploadBlock: GetUploadBlock,
    private val updateToken: UpdateToken,
    private val getBlocksUploadUrl: GetBlocksUploadUrl,
    configurationProvider: ConfigurationProvider,
    canRun: CanRun,
    run: Run,
    done: Done,
) : UploadCoroutineWorker(
    appContext = appContext,
    workerParams = workerParams,
    workManager = workManager,
    broadcastMessages = broadcastMessages,
    getUploadFileLink = getUploadFileLink,
    configurationProvider = configurationProvider,
    canRun = canRun,
    run = run,
    done = done,
) {

    private val url = requireNotNull(inputData.getString(KEY_BLOCK_URL)) { "Block URL is required" }
    private val token = requireNotNull(inputData.getString(KEY_BLOCK_TOKEN)) { "Block token is required" }
    private val index = inputData.getLong(KEY_BLOCK_INDEX, -1L)

    override suspend fun doLimitedRetryUploadWork(uploadFileLink: UploadFileLink): Result = coroutineScope {
        val progress = MutableStateFlow(0L)
        val job = progress
            .onEach { size -> setSize(size) }
            .launchIn(this)
        try {
            val uploadBlock = getUploadBlock(uploadFileLink, index).getOrThrow()
            val (uploadUrl, uploadToken) = uploadBlock.getUrlAndToken(uploadFileLink)
                .onFailure { error ->
                    val retryable = error.isRetryable
                    val canRetry = canRetry()
                    error.log(
                        tag = logTag(),
                        message = """
                            Getting block url and token failed "${error.message}" retryable $retryable,
                            max retries reached ${!canRetry}
                        """.trimIndent(),
                    )
                    return@coroutineScope if (retryable && canRetry) {
                        Result.retry()
                    } else {
                        Result.success(getSizeData(progress.value))
                    }
                }
                .getOrThrow()
            uploadBlock(
                uploadFileLink = uploadFileLink,
                uploadBlock = uploadBlock,
                url = uploadUrl,
                token = uploadToken,
                uploadingProgress = progress,
            )
                .onFailure { error ->
                    val retryable = error.isRetryable || error.handledNotExists()
                    val canRetry = canRetry()
                    error.log(
                        tag = logTag(),
                        message = """
                            Uploading block failed "${error.message}" retryable $retryable,
                            max retries reached ${!canRetry}
                        """.trimIndent(),
                    )
                    return@coroutineScope if (retryable && canRetry) {
                        Result.retry()
                    } else {
                        Result.success(getSizeData(progress.value))
                    }
                }
        } finally {
            job.cancel()
        }
        return@coroutineScope Result.success(getSizeData(progress.value))
    }

    private suspend fun me.proton.core.drive.linkupload.domain.entity.UploadBlock.getUrlAndToken(
        uploadFileLink: UploadFileLink
    ): kotlin.Result<Pair<String, String>> = coRunCatching {
        if (token.isBlank()) {
            this@BlockUploadWorker.url to this@BlockUploadWorker.token
        } else {
            with (getBlocksUploadUrl(uploadFileLink, this@BlockUploadWorker.index).getOrThrow()) {
                blockLinks.firstOrNull()?.let { uploadBlockLink ->
                    uploadBlockLink.url to uploadBlockLink.token
                } ?: thumbnailLink?.let { uploadThumbnailLink ->
                    uploadThumbnailLink.url to uploadThumbnailLink.token
                } ?: error("Upload blocks URL must contain either block link or thumbnail link")
            }
        }
    }

    private suspend fun Throwable.handledNotExists(): Boolean =
        onProtonHttpException { protonCode ->
            if (protonCode == NOT_EXISTS) {
                updateToken(
                    uploadFileLinkId = uploadFileLinkId,
                    index = index,
                    uploadToken = NOT_EXISTS.toString(),
                ).isSuccess
            } else {
                false
            }
        } ?: false

    companion object {
        fun getWorkRequest(
            userId: UserId,
            uploadFileLinkId: Long,
            url: String,
            token: String,
            blockIndex: Long,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(BlockUploadWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, userId.id)
                        .putString(KEY_BLOCK_URL, url)
                        .putString(KEY_BLOCK_TOKEN, token)
                        .putLong(KEY_UPLOAD_FILE_LINK_ID, uploadFileLinkId)
                        .putLong(KEY_BLOCK_INDEX, blockIndex)
                        .build()
                )
                .addTags(listOf(userId.id) + tags)
                .build()
    }
}
