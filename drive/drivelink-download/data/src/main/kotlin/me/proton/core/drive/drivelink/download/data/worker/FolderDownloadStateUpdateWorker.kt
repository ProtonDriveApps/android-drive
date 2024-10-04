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
package me.proton.core.drive.drivelink.download.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.drivelink.download.data.extension.logTag
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_IS_DOWNLOAD_FINISHED
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.usecase.AreAllFilesDownloaded
import me.proton.core.drive.linkdownload.domain.usecase.SetDownloadState
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.util.kotlin.CoreLogger
import java.util.concurrent.TimeUnit

@HiltWorker
class FolderDownloadStateUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val workManager: WorkManager,
    private val setDownloadState: SetDownloadState,
    private val areAllFilesDownloaded: AreAllFilesDownloaded,
) : CoroutineWorker(appContext, workerParams) {
    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)))
    private val folderId = FolderId(shareId, requireNotNull(inputData.getString(KEY_FOLDER_ID)))
    private val isDownloadFinished = inputData.getBoolean(KEY_IS_DOWNLOAD_FINISHED, false)
    private val logTag = folderId.logTag

    override suspend fun doWork(): Result {
        val downloadState = if (isDownloadFinished) DownloadState.Downloaded(emptyList()) else DownloadState.Downloading
        if (!isDownloadFinished || areAllFilesDownloaded(folderId)) {
            CoreLogger.d(logTag, "Setting downloading state to $downloadState")
            setDownloadState(
                linkId = folderId,
                downloadState = downloadState,
            )
        } else {
            workManager.enqueue(getWorkRequest(
                userId = userId,
                folderId = folderId,
                isDownloadFinished = isDownloadFinished,
                tags = tags.toList(),
                delayInSeconds = DEFAULT_RETRY_DELAY
            ))
        }
        return Result.success()
    }

    companion object {

        private const val DEFAULT_RETRY_DELAY = 5L

        fun getWorkRequest(
            userId: UserId,
            folderId: FolderId,
            isDownloadFinished: Boolean,
            tags: List<String>,
            delayInSeconds: Long = 0,
        ): OneTimeWorkRequest {
            return OneTimeWorkRequest.Builder(FolderDownloadStateUpdateWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, userId.id)
                        .putString(KEY_SHARE_ID, folderId.shareId.id)
                        .putString(KEY_FOLDER_ID, folderId.id)
                        .putBoolean(KEY_IS_DOWNLOAD_FINISHED, isDownloadFinished)
                        .build()
                )
                .setInitialDelay(delayInSeconds, TimeUnit.SECONDS)
                .addTags(listOf(userId.id) + tags)
                .build()
        }
    }
}
