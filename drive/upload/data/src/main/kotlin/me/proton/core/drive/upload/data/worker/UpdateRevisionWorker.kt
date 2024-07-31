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

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.api.ProtonApiCode.INVALID_REQUIREMENTS
import me.proton.core.drive.base.domain.api.ProtonApiCode.INVALID_VALUE
import me.proton.core.drive.base.domain.api.ProtonApiCode.NOT_EXISTS
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.data.workmanager.onProtonHttpException
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.upload.data.extension.isRetryable
import me.proton.core.drive.upload.data.extension.retryOrAbort
import me.proton.core.drive.upload.data.extension.uniqueUploadWorkName
import me.proton.core.drive.upload.data.manager.UploadWorkManagerImpl.Companion.TAG_UPLOAD_WORKER
import me.proton.core.drive.upload.data.provider.NetworkTypeProvider
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.drive.upload.domain.usecase.ApplyCacheOption
import me.proton.core.drive.upload.domain.usecase.UpdateRevision
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run
import me.proton.core.network.domain.hasProtonErrorCode
import me.proton.core.util.kotlin.CoreLogger
import java.util.concurrent.TimeUnit

@HiltWorker
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateRevisionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    workManager: WorkManager,
    broadcastMessages: BroadcastMessages,
    getUploadFileLink: GetUploadFileLink,
    uploadErrorManager: UploadErrorManager,
    private val updateRevision: UpdateRevision,
    private val cleanupWorkers: CleanupWorkers,
    private val networkTypeProviders: @JvmSuppressWildcards Map<NetworkTypeProviderType, NetworkTypeProvider>,
    private val applyCacheOption: ApplyCacheOption,
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

    override suspend fun doLimitedRetryUploadWork(uploadFileLink: UploadFileLink): Result {
        uploadFileLink.logWorkState()
        updateRevision(uploadFileLink, runAttemptCount > 0)
            .recoverCatching { cause ->
                if (cause.hasProtonErrorCode(ProtonApiCode.INCOMPATIBLE_STATE)) {
                    CoreLogger.d(logTag(), cause, "Ignoring revision to commit must be a draft")
                } else {
                    throw cause
                }
            }.onFailure { error ->
                val retryable = error.isRetryable
                val canRetry = canRetry()
                error.log(
                    tag = logTag(),
                    message = """
                        Updating revision failed "${error.message}" retryable $retryable, 
                        max retries reached ${!canRetry}
                    """.trimIndent().replace("\n", " ")
                )
                return if (error.handle(uploadFileLink)) {
                    Result.failure()
                } else {
                    retryOrAbort(retryable && canRetry, error, uploadFileLink.name)
                }
            }.onSuccess {
                applyCacheOption(uploadFileLink)
            }
        return Result.success()
    }

    private suspend fun Throwable.handle(uploadFileLink: UploadFileLink): Boolean =
        onProtonHttpException { protonCode ->
            when (protonCode) {
                INVALID_REQUIREMENTS,
                INVALID_VALUE,
                -> true.also { retryGetBlocksUploadUrl(uploadFileLink) }

                NOT_EXISTS,
                -> true.also { recreateFile(uploadFileLink, cleanupWorkers, networkTypeProviders) }

                else -> false
            }
        } ?: false

    private suspend fun retryGetBlocksUploadUrl(uploadFileLink: UploadFileLink) {
        val networkType =
            requireNotNull(networkTypeProviders[uploadFileLink.networkTypeProviderType])
                .get(uploadFileLink.parentLinkId)
        workManager.enqueue(
            GetBlocksUploadUrlWorker.getWorkRequest(
                userId = userId,
                uploadFileLinkId = uploadFileLinkId,
                networkType = networkType,
                tags = listOf(uploadFileLinkId.uniqueUploadWorkName)
            )
        )
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            uploadFileLinkId: Long,
            networkType: NetworkType,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(UpdateRevisionWorker::class.java)
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
                .addTags(listOf(userId.id) + TAG_UPLOAD_WORKER + tags)
                .build()
    }
}
