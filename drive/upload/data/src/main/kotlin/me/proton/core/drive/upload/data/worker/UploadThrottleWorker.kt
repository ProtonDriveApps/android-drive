/*
 * Copyright (c) 2022-2023 Proton AG.
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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.upload.data.extension.uniqueUploadWorkName
import me.proton.core.util.kotlin.CoreLogger

@HiltWorker
class UploadThrottleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val linkUploadRepository: LinkUploadRepository,
    private val workManager: WorkManager,
    private val configurationProvider: ConfigurationProvider,
    private val updateUploadState: UpdateUploadState,
    private val getUploadFileLink: GetUploadFileLink,
) : CoroutineWorker(appContext, workerParams) {
    private val userId = UserId(requireNotNull(inputData.getString(WorkerKeys.KEY_USER_ID)) { "User id is required" })

    override suspend fun doWork(): Result {
        CoreLogger.d(LogTag.UPLOAD, "UploadThrottleWorker started")
        linkUploadRepository.getUploadFileLinks(userId)
            .map { uploadFleLinks ->
                uploadFleLinks.filterNot { uploadFileLink -> uploadFileLink.uriString.isNullOrEmpty() }
            }
            .takeWhile { uploadFileLinks -> uploadFileLinks.isNotEmpty() }
            .collect { uploadFileLinks ->
                val running = uploadFileLinks.filter { uploadFileLink ->
                    getUploadFileLink(uploadFileLink.id).toResult().getOrNull()?.state != UploadState.UNPROCESSED
                }
                val notRunning = uploadFileLinks.subtract(running.toSet())
                val availableUploadSlots = configurationProvider.uploadsInParallel - running.size
                if (notRunning.isNotEmpty() && availableUploadSlots > 0) {
                    notRunning.take(availableUploadSlots).forEach { uploadFileLink ->
                        uploadFileLink.enqueue(userId)
                        updateUploadState(uploadFileLink.id, UploadState.IDLE)
                    }
                }
                CoreLogger.d(
                    tag = LogTag.UPLOAD,
                    message = "UploadThrottleWorker collecting ${uploadFileLinks.map { uploadFileLink -> uploadFileLink.state }.joinToString()}",
                )
            }
        CoreLogger.d(LogTag.UPLOAD, "UploadThrottleWorker finished")
        return Result.success()
    }

    private suspend fun UploadFileLink.enqueue(userId: UserId) =
        if (draftRevisionId.isNotEmpty()) {
            FileUploadFlow.FileAlreadyCreated(
                workManager, userId, id, shouldDeleteSourceUri,
            ).enqueueWork(listOf(id.uniqueUploadWorkName), requireNotNull(uriString))
        } else {
            FileUploadFlow.FromScratch(
                workManager, userId, id, shouldDeleteSourceUri,
            ).enqueueWork(listOf(id.uniqueUploadWorkName), requireNotNull(uriString))
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
