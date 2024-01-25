/*
 * Copyright (c) 2022-2024 Proton AG.
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
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag.UPLOAD
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.upload.data.extension.logTag
import me.proton.core.drive.upload.data.extension.uniqueUploadWorkName
import me.proton.core.drive.upload.data.provider.NetworkTypeProvider
import me.proton.core.drive.upload.domain.usecase.GetNextUploadFileLinks
import me.proton.core.util.kotlin.CoreLogger

@HiltWorker
class UploadThrottleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val workManager: WorkManager,
    private val updateUploadState: UpdateUploadState,
    private val getNextUploadFileLinks: GetNextUploadFileLinks,
    private val networkTypeProviders: @JvmSuppressWildcards Map<NetworkTypeProviderType, NetworkTypeProvider>,
    private val cleanupWorkers: CleanupWorkers,
) : CoroutineWorker(appContext, workerParams) {
    private val userId =
        UserId(requireNotNull(inputData.getString(WorkerKeys.KEY_USER_ID)) { "User id is required" })

    override suspend fun doWork(): Result = runCatching {
        getNextUploadFileLinks(userId).getOrThrow().also { uploadFileLinks ->
            CoreLogger.d(UPLOAD, "UploadThrottleWorker($runAttemptCount) upload ${uploadFileLinks.size} files")
        }.forEach { uploadFileLink ->
            if (uploadFileLink.isNotEnqueued()) {
                uploadFileLink.enqueue(userId, tags).await()
                updateUploadState(uploadFileLink.id, UploadState.IDLE)
                CoreLogger.d(
                    tag = uploadFileLink.logTag(),
                    message = """
                        UploadThrottleWorker($runAttemptCount) enqueue ${uploadFileLink.uriString} 
                        size ${uploadFileLink.size}
                        priority ${uploadFileLink.priority}
                    """.trimIndent().replace("\n", " "),
                )
            } else {
                CoreLogger.w(
                    tag = uploadFileLink.logTag(),
                    message = """
                        UploadThrottleWorker($runAttemptCount) was already enqueued ${uploadFileLink.uriString} 
                        size ${uploadFileLink.size}
                        priority ${uploadFileLink.priority}
                    """.trimIndent().replace("\n", " "),
                )
            }
        }
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { error ->
            error.log(
                UPLOAD,
                "UploadThrottleWorker($runAttemptCount) Cannot enqueue files to be uploaded"
            )
            when {
                error is CancellationException -> throw error
                error.isRetryable -> Result.retry()
                else -> Result.failure()
            }
        }
    )

    private suspend fun UploadFileLink.isNotEnqueued(): Boolean {
        val workInfos = workManager
            .getWorkInfosByTag(id.uniqueUploadWorkName).await()
        return workInfos.none { workInfo -> !workInfo.state.isFinished }
    }

    private suspend fun UploadFileLink.enqueue(
        userId: UserId,
        tags: Set<String> = emptySet(),
    ) = requireNotNull(networkTypeProviders[networkTypeProviderType])
        .get(parentLinkId)
        .let { networkType ->
            val isFileAlreadyCreated = draftRevisionId.isNotEmpty()
            val fileSize = size
            val isFileEmpty = fileSize != null && fileSize.value == 0L
            when {
                isFileAlreadyCreated && isFileEmpty
                -> FileUploadFlow.EmptyFileAlreadyCreated(
                    workManager = workManager,
                    userId = userId,
                    uploadFileLinkId = id,
                    networkType = networkType,
                    cleanupWorkers = cleanupWorkers,
                )

                isFileAlreadyCreated
                -> FileUploadFlow.FileAlreadyCreated(
                    workManager = workManager,
                    userId = userId,
                    uploadFileLinkId = id,
                    shouldDeleteSource = shouldDeleteSourceUri,
                    networkType = networkType,
                )

                !isFileAlreadyCreated && isFileEmpty
                -> FileUploadFlow.EmptyFileFromScratch(
                    workManager = workManager,
                    userId = userId,
                    uploadFileLinkId = id,
                    networkType = networkType,
                    cleanupWorkers = cleanupWorkers,
                )

                !isFileAlreadyCreated
                -> FileUploadFlow.FromScratch(
                    workManager = workManager,
                    userId = userId,
                    uploadFileLinkId = id,
                    shouldDeleteSource = shouldDeleteSourceUri,
                    networkType = networkType,
                )

                else -> error("Unhandled file upload flow ")
            }.enqueueWork(listOf(id.uniqueUploadWorkName) + tags, requireNotNull(uriString))
        }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(UploadThrottleWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(WorkerKeys.KEY_USER_ID, userId.id)
                        .build()
                )
                .addTags(listOf(userId.id) + tags)
                .build()
    }
}
