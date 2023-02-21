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
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadBlock
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.upload.data.extension.getSizeData
import me.proton.core.drive.upload.data.extension.isRetryable
import me.proton.core.drive.upload.data.extension.setSize
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_BLOCK_INDEX
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_BLOCK_TOKEN
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_BLOCK_URL
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.upload.domain.usecase.UploadBlock

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
    configurationProvider: ConfigurationProvider,
) : UploadCoroutineWorker(
    appContext = appContext,
    workerParams = workerParams,
    workManager = workManager,
    broadcastMessages = broadcastMessages,
    getUploadFileLink = getUploadFileLink,
    configurationProvider = configurationProvider,
) {

    private val url = requireNotNull(inputData.getString(KEY_BLOCK_URL)) { "Block URL is required" }
    private val token = requireNotNull(inputData.getString(KEY_BLOCK_TOKEN)) { "Block token is required" }
    private val index = inputData.getLong(KEY_BLOCK_INDEX, -1L)

    override suspend fun doUploadWork(uploadFileLink: UploadFileLink): Result = coroutineScope {
        val progress = MutableStateFlow(0L)
        val job = progress
            .onEach { size -> setSize(size) }
            .launchIn(this)
        try {
            val uploadBlock = getUploadBlock(uploadFileLink, index).getOrThrow()
            uploadBlock(
                uploadFileLink = uploadFileLink,
                uploadBlock = uploadBlock,
                url = url,
                token = token,
                uploadingProgress = progress,
            )
                .onFailure { error ->
                    error.log(
                        tag = logTag(),
                        message = "Uploading block failed \"${error.message}\" retryable ${error.isRetryable}",
                    )
                    return@coroutineScope if (error.isRetryable) Result.retry() else Result.success(getSizeData(progress.value))
                }
        } finally {
            job.cancel()
        }
        return@coroutineScope Result.success(getSizeData(progress.value))
    }

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
