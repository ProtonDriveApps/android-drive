/*
 * Copyright (c) 2024 Proton AG.
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.entity.LoggerLevel
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidSDKQueueFullConstraint
import me.proton.core.drive.feature.flag.domain.usecase.IsFeatureFlagEnabled
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.UpdateName
import me.proton.core.drive.upload.data.exception.UploadCleanupException
import me.proton.core.drive.upload.data.extension.isRetryable
import me.proton.core.drive.upload.data.extension.retryOrAbort
import me.proton.core.drive.upload.data.provider.NetworkTypeProvider
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_URI_STRING
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.upload.domain.exception.ContentSizeChangedException
import me.proton.core.drive.upload.domain.exception.SdkQueueTimeoutException
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.drive.upload.domain.usecase.CreateNewFileSdk
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import me.proton.core.drive.worker.domain.entity.Constraint
import me.proton.core.drive.worker.domain.usecase.WorkerRuns

@HiltWorker
@OptIn(ExperimentalCoroutinesApi::class)
class CreateNewFileSdkWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    workManager: WorkManager,
    broadcastMessages: BroadcastMessages,
    getUploadFileLink: GetUploadFileLink,
    uploadErrorManager: UploadErrorManager,
    private val createNewFileSdk: CreateNewFileSdk,
    private val isFeatureFlagEnabled: IsFeatureFlagEnabled,
    configurationProvider: ConfigurationProvider,
    updateName: UpdateName,
    private val networkTypeProviders: @JvmSuppressWildcards Map<NetworkTypeProviderType, NetworkTypeProvider>,
    workerRuns: WorkerRuns,
) : CommonCreateFileWorker(
    appContext = appContext,
    workerParams = workerParams,
    workManager = workManager,
    broadcastMessages = broadcastMessages,
    getUploadFileLink = getUploadFileLink,
    uploadErrorManager = uploadErrorManager,
    configurationProvider = configurationProvider,
    updateName = updateName,
    workerRuns = workerRuns,
) {
    private val uriString = requireNotNull(inputData.getString(KEY_URI_STRING))
    private val sdkQueue = MutableSharedFlow<Constraint.Runtime>(replay = 1)
    override val runtimeConstraints: Flow<Constraint.Runtime> =
        merge(sdkQueue, networkNotAllowed(networkTypeProviders))

    override suspend fun doLimitedRetryUploadWork(uploadFileLink: UploadFileLink): Result {
        uploadFileLink.logWorkState("Creating upload file via SDK (size = ${uploadFileLink.size})")
        awaitAllowedNetwork(networkTypeProviders, uploadFileLink)
        return createNewFileSdk(
            uploadFileLink = uploadFileLink,
            uriString = uriString,
        ).fold(
            onFailure = { error ->
                when (error) {
                    is FileNotFoundException -> {
                        setUploadAsCancelled()
                        error.log(
                            tag = logTag(),
                            message = "File does not exist anymore, cancelling upload",
                            level = LoggerLevel.WARNING,
                        )
                        throw UploadCleanupException(error, uploadFileLink.name)
                    }
                    is ContentSizeChangedException -> uploadFileLink.retryOrAbort(
                        retryable = true,
                        canRetry = canRetry(),
                        error = error,
                        message = "Content size changed while creating upload file via SDK",
                    )
                    is SdkQueueTimeoutException ->
                        if (isFeatureFlagEnabled(driveAndroidSDKQueueFullConstraint(userId)) && canBlock()) {
                            sdkQueue.emit(Constraint.Runtime.SdkQueueFull)
                            awaitCancellation()
                        } else {
                            uploadFileLink.retryOrAbort(
                                retryable = true,
                                canRetry = canRetry(),
                                error = error,
                                message = "Enqueue timed out while creating upload file via SDK",
                            )
                        }
                    else -> {
                        val retryable = error.isRetryable || error.handle(uploadFileLink)
                        uploadFileLink.retryOrAbort(
                            retryable = retryable,
                            canRetry = canRetry(),
                            error = error,
                            message = "Creating upload file via SDK failed",
                        )
                    }
                }
            },
            onSuccess = {
                Result.success()
            }
        )
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            uploadFileLinkId: Long,
            uriString: String,
            networkType: NetworkType,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(CreateNewFileSdkWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, userId.id)
                        .putLong(KEY_UPLOAD_FILE_LINK_ID, uploadFileLinkId)
                        .putString(KEY_URI_STRING, uriString)
                        .build()
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(networkType)
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
