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

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.supervisorScope
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLinksCount
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLinksWithUriByPriority
import me.proton.core.drive.upload.domain.usecase.AnnounceUploadEvent
import me.proton.core.drive.upload.domain.usecase.GetUploadProgress
import me.proton.core.util.kotlin.CoreLogger
import kotlin.time.Duration.Companion.seconds

@HiltWorker
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Suppress("LongParameterList")
class UploadEventWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val linkUploadRepository: LinkUploadRepository,
    private val getUploadProgress: GetUploadProgress,
    private val announceUploadEvent: AnnounceUploadEvent,
    private val getUploadFileLinksCount: GetUploadFileLinksCount,
    private val getUploadFileLinksWithUriByPriority: GetUploadFileLinksWithUriByPriority,
    private val configurationProvider: ConfigurationProvider,
    private val notifier: Notifier,
) : CoroutineWorker(appContext, workerParams) {

    interface Notifier : (UserId, Event.Upload) -> Pair<Int, Notification>

    private val userId =
        UserId(requireNotNull(inputData.getString(WorkerKeys.KEY_USER_ID)) { "User id is required" })

    override suspend fun doWork(): Result = supervisorScope {
        CoreLogger.d(
            tag = LogTag.NOTIFICATION,
            message = "Starting upload notification event worker",
        )
        setForeground(createForegroundInfo())
        try {
            val previousIds = mutableSetOf<Long>()
            combine(getUploadFileLinksWithUriByPriority(
                userId = userId,
                states = setOf(
                    UploadState.CREATING_NEW_FILE,
                    UploadState.SPLITTING_URI_TO_BLOCKS,
                    UploadState.ENCRYPTING_BLOCKS,
                    UploadState.GETTING_UPLOAD_LINKS,
                    UploadState.UPLOADING_BLOCKS,
                    UploadState.UPDATING_REVISION,
                ),
                count = configurationProvider.uploadsInParallel * 2,
            ), getUploadFileLinksCount(userId)) { uploadFileLinks, count ->
                uploadFileLinks to count
            }
                // do not collect forever from database
                .takeWhile { (_, count) -> count.totalWithAnnounce != 0 }
                .map { (uploadFileLinks, _) -> uploadFileLinks.map { uploadFileLink -> uploadFileLink.id } }
                .distinctUntilChanged()
                .onEach { uploadFileLinksIds ->
                    (uploadFileLinksIds - previousIds).forEach { uploadFileLinkId ->
                        async { announceProgress(uploadFileLinkId) }
                    }
                    previousIds.clear()
                    previousIds.addAll(uploadFileLinksIds)
                }.onCompletion {
                    CoreLogger.d(LogTag.NOTIFICATION, "Stop observing upload file links")
                }.launchIn(this)
        } catch (e: CancellationException) {
            CoreLogger.d(LogTag.NOTIFICATION, e, e.message.orEmpty())
            cancel(e)
        }
        CoreLogger.d(LogTag.NOTIFICATION, "Upload notification event worker finished")
        Result.success()
    }

    private suspend fun announceProgress(uploadFileLinkId: Long) {
        linkUploadRepository.getUploadFileLinkFlow(uploadFileLinkId)
            .takeWhile { uploadFileLink -> uploadFileLink != null }
            .filterNotNull()
            .distinctUntilChangedBy { uploadFileLink -> uploadFileLink.state }
            .transformLatest { uploadFileLink ->
                val flow = getUploadProgress(uploadFileLink)
                if (flow != null) {
                    emitAll(
                        flow
                            .sample(1.seconds)
                            // do not collect forever from database or worker livedata
                            .takeWhile { percentage -> percentage <= Percentage(100) }
                            .mapLatest { percentage ->
                                uploadFileLink to uploadFileLink.toEvent(percentage)
                            }
                    )
                } else {
                    emit(uploadFileLink to uploadFileLink.toEvent())
                }
            }.collectLatest { (uploadFileLink, uploadEvent) ->
                announceUploadEvent(
                    uploadFileLink = uploadFileLink,
                    uploadEvent = uploadEvent,
                )
            }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val (id, notification) = notifier(
            userId,
            Event.Upload(
                state = Event.Upload.UploadState.NEW_UPLOAD,
                uploadFileLinkId = -1,
                percentage = Percentage(0),
                shouldShow = false,
            )
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    companion object {

        fun getWorkRequest(
            userId: UserId,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(UploadEventWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(WorkerKeys.KEY_USER_ID, userId.id)
                        .build()
                )
                .addTags(listOf(userId.id) + tags)
                .build()
    }

    private fun UploadFileLink.toEvent(percentage: Percentage = Percentage(0)) =
        Event.Upload(
            state = this.state.toNotificationUploadState(),
            uploadFileLinkId = id,
            percentage = this.state.toPercentage(percentage),
            shouldShow = shouldAnnounceEvent,
        )

    private fun UploadState.toPercentage(percentage: Percentage): Percentage = when (this) {
        UploadState.UPLOADING_BLOCKS -> percentage
        UploadState.UPDATING_REVISION -> Percentage(100)
        else -> Percentage(0)
    }

    private fun UploadState.toNotificationUploadState() = when (this) {
        UploadState.UNPROCESSED -> Event.Upload.UploadState.NEW_UPLOAD
        else -> Event.Upload.UploadState.UPLOADING
    }
}
