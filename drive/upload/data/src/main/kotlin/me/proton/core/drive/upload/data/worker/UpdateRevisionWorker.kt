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
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.api.ProtonApiCode.INVALID_REQUIREMENTS
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.data.workmanager.onProtonHttpException
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.upload.data.extension.isRetryable
import me.proton.core.drive.upload.data.extension.retryOrAbort
import me.proton.core.drive.upload.data.extension.uniqueUploadWorkName
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.upload.domain.usecase.UpdateRevision
import java.util.concurrent.TimeUnit

@HiltWorker
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateRevisionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    workManager: WorkManager,
    broadcastMessages: BroadcastMessages,
    getUploadFileLink: GetUploadFileLink,
    private val updateRevision: UpdateRevision,
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
        updateRevision(uploadFileLink)
            .onFailure { error ->
                error.log(
                    tag = logTag(),
                    message = """Updating revision failed "${error.message}" retryable ${error.isRetryable}"""
                )
                return if (error.handle()) {
                    Result.failure()
                } else {
                    retryOrAbort(error.isRetryable, error, uploadFileLink.name)
                }
            }
        return Result.success()
    }

    private fun Throwable.handle(): Boolean =
        onProtonHttpException { protonCode ->
            if (protonCode == INVALID_REQUIREMENTS) {
                retryGetBlocksUploadUrl()
                return@onProtonHttpException true
            }
            return@onProtonHttpException false
        } ?: false

    private fun retryGetBlocksUploadUrl() {
        workManager.enqueue(
            GetBlocksUploadUrlWorker.getWorkRequest(
                userId = userId,
                uploadFileLinkId = uploadFileLinkId,
                tags = listOf(uploadFileLinkId.uniqueUploadWorkName)
            )
        )
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            uploadFileLinkId: Long,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(UpdateRevisionWorker::class.java)
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
