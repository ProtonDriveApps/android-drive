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
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.block.domain.entity.UploadBlocksUrl
import me.proton.core.drive.link.domain.entity.Link.Companion.THUMBNAIL_INDEX
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.upload.data.extension.isRetryable
import me.proton.core.drive.upload.data.extension.logTag
import me.proton.core.drive.upload.data.extension.retryOrAbort
import me.proton.core.drive.upload.data.extension.uniqueUploadWorkName
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.upload.domain.usecase.GetBlocksUploadUrl
import java.util.concurrent.TimeUnit

@HiltWorker
@OptIn(ExperimentalCoroutinesApi::class)
class GetBlocksUploadUrlWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    workManager: WorkManager,
    broadcastMessages: BroadcastMessages,
    getUploadFileLink: GetUploadFileLink,
    private val getBlocksUploadUrl: GetBlocksUploadUrl,
    configurationProvider: ConfigurationProvider,
) : UploadCoroutineWorker(
    appContext = appContext,
    workerParams = workerParams,
    workManager = workManager,
    broadcastMessages = broadcastMessages,
    getUploadFileLink = getUploadFileLink,
    configurationProvider = configurationProvider,
) {

    override suspend fun doUploadWork(uploadFileLink: UploadFileLink): Result {
        getBlocksUploadUrl(uploadFileLink)
            .onFailure { error ->
                error.log(
                    tag = uploadFileLink.logTag(),
                    message = """Get blocks URL failed "${error.message}" retryable ${error.isRetryable}"""
                )
                return retryOrAbort(error.isRetryable, error, uploadFileLink.name)
            }
            .onSuccess { uploadBlocksUrl ->
                uploadBlocks(uploadBlocksUrl)
            }
        return Result.success()
    }

    @SuppressLint("EnqueueWork")
    private fun uploadBlocks(uploadBlocksUrl: UploadBlocksUrl) {
        val uploadTag = listOf(uploadFileLinkId.uniqueUploadWorkName)
        (uploadBlocksUrl.blockLinks.mapIndexed { index, uploadLink ->
            BlockUploadWorker.getWorkRequest(
                userId = userId,
                url = uploadLink.url,
                token = uploadLink.token,
                uploadFileLinkId = uploadFileLinkId,
                blockIndex = index + 1L,
                tags = uploadTag
            )
        } + listOfNotNull(uploadBlocksUrl.thumbnailLink?.let { uploadLink ->
            BlockUploadWorker.getWorkRequest(
                userId = userId,
                url = uploadLink.url,
                token = uploadLink.token,
                uploadFileLinkId = uploadFileLinkId,
                blockIndex = THUMBNAIL_INDEX,
                tags = uploadTag
            )
        }))
            .chunked(configurationProvider.uploadBlocksInParallel)
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
                    .then(
                        UpdateRevisionWorker.getWorkRequest(userId, uploadFileLinkId, uploadTag)
                    )
                    .then(
                        UploadSuccessCleanupWorker.getWorkRequest(userId, uploadFileLinkId, uploadTag)
                    )
                    .enqueue()
            }
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            uploadFileLinkId: Long,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(GetBlocksUploadUrlWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, userId.id)
                        .putLong(KEY_UPLOAD_FILE_LINK_ID, uploadFileLinkId)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTags(listOf(userId.id) + tags)
                .build()
    }
}
