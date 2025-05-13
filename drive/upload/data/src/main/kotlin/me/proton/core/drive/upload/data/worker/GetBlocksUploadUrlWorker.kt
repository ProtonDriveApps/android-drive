/*
 * Copyright (c) 2021-2024 Proton AG.
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
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.await
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.entity.LoggerLevel.WARNING
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.data.workmanager.onProtonHttpException
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.block.domain.entity.UploadBlocksUrl
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.upload.data.extension.isRetryable
import me.proton.core.drive.upload.data.extension.logTag
import me.proton.core.drive.upload.data.extension.retryOrAbort
import me.proton.core.drive.upload.data.extension.uniqueUploadWorkName
import me.proton.core.drive.upload.data.provider.NetworkTypeProvider
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.drive.upload.domain.usecase.GetBlocksUploadUrl
import me.proton.core.drive.upload.domain.usecase.UploadMetricsNotifier
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run
import java.util.concurrent.TimeUnit

@HiltWorker
@OptIn(ExperimentalCoroutinesApi::class)
class GetBlocksUploadUrlWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    workManager: WorkManager,
    broadcastMessages: BroadcastMessages,
    getUploadFileLink: GetUploadFileLink,
    uploadErrorManager: UploadErrorManager,
    private val getBlocksUploadUrl: GetBlocksUploadUrl,
    private val cleanUpWorkers: CleanupWorkers,
    private val networkTypeProviders: @JvmSuppressWildcards Map<NetworkTypeProviderType, NetworkTypeProvider>,
    configurationProvider: ConfigurationProvider,
    uploadMetricsNotifier: UploadMetricsNotifier,
    canRun: CanRun,
    run: Run,
    done: Done,
) : UploadCoroutineWorker(
    appContext = appContext,
    workerParams = workerParams,
    workManager = workManager,
    broadcastMessages = broadcastMessages,
    getUploadFileLink = getUploadFileLink,
    uploadErrorManager = uploadErrorManager,
    configurationProvider = configurationProvider,
    uploadMetricsNotifier = uploadMetricsNotifier,
    canRun = canRun,
    run = run,
    done = done,
) {

    override suspend fun doLimitedRetryUploadWork(uploadFileLink: UploadFileLink): Result {
        uploadFileLink.logWorkState("get block urls")
        return getBlocksUploadUrl(uploadFileLink).fold(
            onFailure = { error ->
                if (error.handle(uploadFileLink)) {
                    uploadMetricsNotifier(
                        uploadFileLink = uploadFileLink,
                        isSuccess = false,
                        throwable = error,
                    )
                    Result.failure()
                } else {
                    uploadFileLink.retryOrAbort(
                        retryable = error.isRetryable,
                        canRetry = canRetry(),
                        error = error,
                        message = "Get blocks URL failed"
                    )
                }
            },
            onSuccess = { uploadBlocksUrl ->
                uploadBlocks(uploadBlocksUrl, uploadFileLink).fold(
                    onFailure = { error ->
                        when {
                            error is CancellationException -> throw error
                            error.isRetryable -> {
                                error.log(
                                    tag = uploadFileLink.logTag(),
                                    message = "Cannot enqueue files to be uploaded, will retry",
                                    level = WARNING,
                                )
                                Result.retry()
                            }
                            else -> {
                                error.log(
                                    tag = uploadFileLink.logTag(),
                                    message = "Cannot enqueue files to be uploaded"
                                )
                                Result.failure()
                            }
                        }
                    },
                    onSuccess = { Result.success() }
                )
            }
        )
    }

    @SuppressLint("EnqueueWork")
    private suspend fun uploadBlocks(
        uploadBlocksUrl: UploadBlocksUrl,
        uploadFileLink: UploadFileLink,
    ) = runCatching{
        uploadFileLink.logWorkState("enqueueing worker")
        val uploadTag = listOf(uploadFileLinkId.uniqueUploadWorkName)
        val networkType =
            requireNotNull(networkTypeProviders[uploadFileLink.networkTypeProviderType])
                .get(uploadFileLink.parentLinkId)
        require(isNotEnqueued()){ "Workers are already enqueued" }
        (uploadBlocksUrl.blockLinks.mapIndexed { index, uploadLink ->
            BlockUploadWorker.getWorkRequest(
                userId = userId,
                url = uploadLink.url,
                token = uploadLink.token,
                uploadFileLinkId = uploadFileLinkId,
                blockIndex = index + 1L,
                networkType = networkType,
                tags = uploadTag,
            )
        } + (uploadBlocksUrl.thumbnailLinks.mapIndexed { index, uploadLink ->
            val blockIndex = when (index) {
                0 -> Block.THUMBNAIL_DEFAULT_INDEX
                1 -> Block.THUMBNAIL_PHOTO_INDEX
                else -> error("Unexpected number of thumbnails")
            }
            BlockUploadWorker.getWorkRequest(
                userId = userId,
                url = uploadLink.url,
                token = uploadLink.token,
                uploadFileLinkId = uploadFileLinkId,
                blockIndex = blockIndex,
                networkType = networkType,
                tags = uploadTag,
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
            }.let { continuations ->
                if (continuations.isNotEmpty()) {
                    WorkContinuation.combine(continuations)
                        .then(
                            UpdateRevisionWorker.getWorkRequest(
                                userId = userId,
                                uploadFileLinkId = uploadFileLinkId,
                                networkType = networkType,
                                tags = uploadTag,
                            )
                        )
                } else {
                    workManager.beginWith(
                        UpdateRevisionWorker.getWorkRequest(
                            userId = userId,
                            uploadFileLinkId = uploadFileLinkId,
                            networkType = networkType,
                            tags = uploadTag,
                        )
                    )
                }.then(cleanUpWorkers(userId, uploadFileLink, uploadTag))
                    .enqueue().await()
            }
    }

    private suspend fun Throwable.handle(uploadFileLink: UploadFileLink): Boolean =
        onProtonHttpException { protonData ->
            when (protonData.code) {
                ProtonApiCode.NOT_EXISTS,
                -> true.also { recreateFile(uploadFileLink, cleanUpWorkers, networkTypeProviders) }

                else -> false
            }
        } ?: false

    private suspend fun isNotEnqueued(): Boolean =
        workManager.getWorkInfosByTag(uploadFileLinkId.uniqueUploadWorkName).await()
            .filter { workInfo -> workInfo.tags.contains(BlockUploadWorker.TAG) }
            .none { workInfo -> !workInfo.state.isFinished }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            uploadFileLinkId: Long,
            networkType: NetworkType,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(GetBlocksUploadUrlWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(networkType)
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
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTags(listOf(userId.id) + tags)
                .build()
    }
}
