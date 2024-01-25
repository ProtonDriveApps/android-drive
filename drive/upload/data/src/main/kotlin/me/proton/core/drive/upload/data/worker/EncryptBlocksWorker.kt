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
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.upload.data.extension.isRetryable
import me.proton.core.drive.upload.data.extension.logTag
import me.proton.core.drive.upload.data.extension.retryOrAbort
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_SHOULD_DELETE_SOURCE
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_URI_STRING
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.drive.upload.domain.usecase.SplitFileToBlocksAndEncrypt
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run
import java.util.concurrent.TimeUnit

@HiltWorker
@OptIn(ExperimentalCoroutinesApi::class)
class EncryptBlocksWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    workManager: WorkManager,
    broadcastMessages: BroadcastMessages,
    getUploadFileLink: GetUploadFileLink,
    private val getShare: GetShare,
    uploadErrorManager: UploadErrorManager,
    private val splitFileToBlocksAndEncrypt: SplitFileToBlocksAndEncrypt,
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
    uploadErrorManager = uploadErrorManager,
    configurationProvider = configurationProvider,
    canRun = canRun,
    run = run,
    done = done,
) {

    private val uriString = requireNotNull(inputData.getString(KEY_URI_STRING))
    private val shouldDeleteSource = inputData.getBoolean(KEY_SHOULD_DELETE_SOURCE, false)

    override suspend fun doLimitedRetryUploadWork(uploadFileLink: UploadFileLink): Result {
        uploadFileLink.logWorkState("split (size = ${uploadFileLink.size}) and encrypt")
        splitFileToBlocksAndEncrypt(
            uploadFileLink = uploadFileLink,
            uriString = uriString,
            shouldDeleteSource = shouldDeleteSource,
            includePhotoThumbnail = uploadFileLink.isBiggerThenPhotoThumbnail && uploadFileLink.isImagePhoto(),
        )
            .onFailure { error ->
                val retryable = error.isRetryable
                val canRetry = canRetry()
                error.log(
                    tag = uploadFileLink.logTag(),
                    message = """
                        Encrypting blocks failed "${error.message}" retryable $retryable, 
                        max retries reached ${!canRetry}
                    """.trimIndent().replace("\n", " ")
                )
                return retryOrAbort(retryable && canRetry, error, uploadFileLink.name)
            }
        return Result.success()
    }

    private val UploadFileLink.isBiggerThenPhotoThumbnail: Boolean get() = mediaResolution?.let { resolution ->
        resolution.width > configurationProvider.thumbnailPhoto.maxWidth ||
                resolution.height > configurationProvider.thumbnailPhoto.maxHeight
    } ?: false

    private suspend fun UploadFileLink.isPhoto(): Boolean {
        val share = getShare(shareId, flowOf(false)).toResult().getOrThrow()
        return share.type == Share.Type.PHOTO
    }

    private val UploadFileLink.isImage: Boolean get() = mimeType.toFileTypeCategory() == FileTypeCategory.Image

    private suspend fun UploadFileLink.isImagePhoto(): Boolean = isPhoto() && isImage

    companion object {
        fun getWorkRequest(
            userId: UserId,
            uploadFileLinkId: Long,
            uriString: String,
            shouldDeleteSource: Boolean = false,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(EncryptBlocksWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, userId.id)
                        .putLong(KEY_UPLOAD_FILE_LINK_ID, uploadFileLinkId)
                        .putString(KEY_URI_STRING, uriString)
                        .putBoolean(KEY_SHOULD_DELETE_SOURCE, shouldDeleteSource)
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
