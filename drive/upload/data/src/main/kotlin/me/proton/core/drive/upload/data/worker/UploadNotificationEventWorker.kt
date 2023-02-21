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
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.usecase.AnnounceEvent
import me.proton.core.drive.upload.domain.usecase.GetUploadProgress
import me.proton.core.util.kotlin.CoreLogger
import kotlin.time.Duration.Companion.seconds

@HiltWorker
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class UploadNotificationEventWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val linkUploadRepository: LinkUploadRepository,
    private val getUploadProgress: GetUploadProgress,
    private val announceEvent: AnnounceEvent,
) : CoroutineWorker(appContext, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(WorkerKeys.KEY_USER_ID)) { "User id is required" })

    override suspend fun doWork(): Result = supervisorScope {
        val uploadFileLinks = linkUploadRepository.getUploadFileLinks(userId).first()
        CoreLogger.d(
            tag = LogTag.NOTIFICATION,
            message = "Starting upload notification event worker for ${uploadFileLinks.size} files",
        )
        val deferred = uploadFileLinks.map { uploadFileLink ->
            async {
                val progressScope = CoroutineScope(Job() + Dispatchers.IO)
                linkUploadRepository.getUploadFileLinkFlow(uploadFileLink.id)
                    .transformWhile { uploadFileLink ->
                        if (uploadFileLink != null && uploadFileLink.state != UploadState.UNPROCESSED && uploadFileLink.state != UploadState.IDLE) emit(uploadFileLink)
                        if (uploadFileLink == null) progressScope.cancel()
                        uploadFileLink != null
                    }
                    .distinctUntilChangedBy { uploadFileLink -> uploadFileLink.state }
                    .transformLatest { uploadFileLink ->
                        progressScope.launch {
                            val flow = getUploadProgress(uploadFileLink)
                            flow?.let {
                                emitAll(
                                    flow
                                        .sample(2.seconds)
                                        .mapLatest { percentage ->
                                            uploadFileLink.toNotificationEvent(percentage)
                                        }
                                )
                            } ?: emit(uploadFileLink.toNotificationEvent())
                        }
                    }
                    .collectLatest { event ->
                        announceEvent(
                            userId = userId,
                            notificationEvent = event,
                        )
                    }
            }
        }
        try {
            deferred.awaitAll()
        } catch (e: CancellationException) {
            CoreLogger.d(LogTag.NOTIFICATION, e, e.message.orEmpty())
            cancel()
        }
        CoreLogger.d(LogTag.NOTIFICATION, "Upload notification event worker finished")
        return@supervisorScope Result.success()
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(UploadNotificationEventWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(WorkerKeys.KEY_USER_ID, userId.id)
                        .build()
                )
                .addTags(listOf(userId.id) + tags)
                .build()
    }

    fun UploadFileLink.toNotificationEvent(percentage: Percentage = Percentage(0)) = NotificationEvent.Upload(
        state = this.state.toNotificationUploadState(),
        uploadFileLinkId = id,
        percentage = this.state.toPercentage(percentage)
    )

    private fun UploadState.toPercentage(percentage: Percentage): Percentage = when (this) {
        UploadState.UPLOADING_BLOCKS -> percentage
        UploadState.UPDATING_REVISION -> Percentage(100)
        else -> Percentage(0)
    }

    private fun UploadState.toNotificationUploadState() = when (this) {
        UploadState.UNPROCESSED -> NotificationEvent.Upload.UploadState.NEW_UPLOAD
        else -> NotificationEvent.Upload.UploadState.UPLOADING
    }
}
