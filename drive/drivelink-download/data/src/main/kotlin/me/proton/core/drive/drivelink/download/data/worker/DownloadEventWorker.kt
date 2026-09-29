/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.drivelink.download.data.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.download.domain.entity.DownloadFileLink
import me.proton.core.drive.drivelink.download.domain.entity.revisionUid
import me.proton.core.drive.drivelink.download.domain.manager.DownloadManager
import me.proton.core.drive.drivelink.download.domain.repository.DownloadFileRepository
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidDownloadFileProgressNotificationDisabled
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.worker.data.usecase.TransferDataNotifier
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.util.kotlin.CoreLogger
import kotlin.time.Duration.Companion.seconds

@HiltWorker
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DownloadEventWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadFileRepository: DownloadFileRepository,
    private val downloadManager: DownloadManager,
    private val announceEvent: AnnounceEvent,
    private val transferDataNotifier: TransferDataNotifier,
    private val appLifecycleProvider: AppLifecycleProvider,
    private val getFeatureFlag: GetFeatureFlag,
) : CoroutineWorker(appContext, workerParams) {

    private val userId =
        UserId(requireNotNull(inputData.getString(WorkerKeys.KEY_USER_ID)) { "User id is required" })

    private val transferDataNotification: kotlin.Result<Pair<NotificationId, Notification>> =
        transferDataNotifier(userId = userId, event = Event.TransferData)

    override suspend fun doWork(): Result {
        val isKillSwitchOn = getFeatureFlag(
            driveAndroidDownloadFileProgressNotificationDisabled(userId)
        ).on
        if (isKillSwitchOn) {
            CoreLogger.d(LogTag.NOTIFICATION, "Kill switch is on, skipping download event worker")
            return Result.success()
        }
        if (appLifecycleProvider.state.value != AppLifecycleProvider.State.Foreground) {
            CoreLogger.d(LogTag.NOTIFICATION, "App is not in foreground, skipping download event worker")
            return Result.success()
        }
        CoreLogger.d(LogTag.NOTIFICATION, "Starting download event worker")
        setForeground()
        try {
            supervisorScope {
                val progressMap = MutableStateFlow<Map<Long, Percentage>>(emptyMap())
                val previousLinks = mutableMapOf<Long, DownloadFileLink>()
                val fileProgressJobs = mutableMapOf<Long, Job>()

                val progressMapJob = progressMap
                    .sample(1.seconds)
                    .onEach { map ->
                        val count = map.size
                        val aggregate = if (count > 0) {
                            Percentage(map.values.sumOf { it.value.toDouble() }.toFloat() / count)
                        } else {
                            Percentage(0f)
                        }
                        announceEvent(
                            userId = userId,
                            event = Event.DownloadFileProgress(
                                downloadingCount = count,
                                progress = aggregate,
                            ),
                        )
                    }
                    .launchIn(this)

                combine(
                    downloadFileRepository.getCountFlow(userId, DownloadFileLink.State.RUNNING),
                    downloadFileRepository.getCountFlow(userId, DownloadFileLink.State.IDLE),
                ) { running, idle -> running + idle }
                    .takeWhile { total -> total > 0 }
                    .onEach {
                        val running = downloadFileRepository.getAllWithState(userId, DownloadFileLink.State.RUNNING)
                        val currentIds = running.map { it.id }.toSet()
                        (previousLinks.keys - currentIds).forEach { id ->
                            fileProgressJobs.remove(id)?.cancel()
                            progressMap.update { it - id }
                        }
                        (currentIds - previousLinks.keys).forEach { id ->
                            val link = running.first { it.id == id }
                            fileProgressJobs[id] = async {
                                downloadManager.getProgressFlow(link.revisionUid)
                                    ?.collect { percentage ->
                                        progressMap.update { it + (id to percentage) }
                                    }
                            }
                        }
                        previousLinks.clear()
                        previousLinks.putAll(running.associateBy { it.id })
                    }
                    .onCompletion {
                        CoreLogger.d(LogTag.NOTIFICATION, "Stop observing download file links")
                        fileProgressJobs.values.forEach { it.cancel() }
                        progressMapJob.cancel()
                    }
                    .launchIn(this)
            }
        } catch (e: CancellationException) {
            CoreLogger.d(LogTag.NOTIFICATION, e, e.message.orEmpty())
            throw e
        } finally {
            withContext(NonCancellable) {
                announceEvent(
                    userId = userId,
                    event = Event.DownloadFileProgress(
                        downloadingCount = 0,
                        progress = Percentage(0f),
                    ),
                )
            }
            transferDataNotification.getOrNull(LogTag.NOTIFICATION)
                ?.first
                ?.let { notificationId -> transferDataNotifier.dismissNotification(notificationId) }
        }
        CoreLogger.d(LogTag.NOTIFICATION, "Download event worker finished")
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createForegroundInfo().getOrThrow()

    private fun createForegroundInfo(): kotlin.Result<ForegroundInfo> = coRunCatching {
        val (notificationId, notification) = transferDataNotification.getOrThrow()
        ForegroundInfo(
            notificationId.id,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private suspend fun setForeground() {
        createForegroundInfo()
            .getOrNull(LogTag.NOTIFICATION, "Failed creating foreground info")
            ?.let { foregroundInfo -> setForeground(foregroundInfo) }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "DownloadEventWorker"

        fun getWorkRequest(userId: UserId): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(DownloadEventWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(WorkerKeys.KEY_USER_ID, userId.id)
                        .build()
                )
                .build()
    }
}
