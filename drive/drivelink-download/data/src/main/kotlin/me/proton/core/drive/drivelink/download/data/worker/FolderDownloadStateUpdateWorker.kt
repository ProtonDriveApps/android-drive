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
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.await
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.download.data.BuildConfig
import me.proton.core.drive.drivelink.download.data.extension.logTag
import me.proton.core.drive.drivelink.download.data.extension.uniqueFolderWorkName
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_IS_DOWNLOAD_FINISHED
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_ROOT_FOLDER_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.folder.domain.usecase.GetDescendants
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
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
    private val getDriveLink: GetDriveLink,
    private val getDescendants: GetDescendants,
) : CoroutineWorker(appContext, workerParams) {
    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)))
    private val folderId = FolderId(shareId, requireNotNull(inputData.getString(KEY_FOLDER_ID)))
    private val rootFolderId = inputData.getString(KEY_ROOT_FOLDER_ID)?.let {
        FolderId(shareId, it)
    }
    private val isDownloadFinished = inputData.getBoolean(KEY_IS_DOWNLOAD_FINISHED, false)
    private val logTag = folderId.logTag
    override suspend fun doWork(): Result {
        val downloadState = if (isDownloadFinished) DownloadState.Downloaded() else DownloadState.Downloading
        if (!isDownloadFinished || areAllFilesDownloaded(folderId)) {
            CoreLogger.d(logTag, "Setting downloading state to ${downloadState::class.simpleName}")
            setDownloadState(
                linkId = folderId,
                downloadState = downloadState,
            )
        } else {
            if (finishedWorkers()) {
                // download is finished but not all files are downloaded
                if (runAttemptCount == 0) {
                    CoreLogger.d(
                        tag = logTag,
                        message = "Download is finished but not all files are downloaded, re-checking once",
                    )
                    return Result.retry()
                }
                if (BuildConfig.DEBUG) {
                    val folderDriveLink = getDriveLink(folderId).toResult().getOrThrow()
                    CoreLogger.d(
                        tag = logTag,
                        message = "Download is finished but not all files are downloaded, re-downloading",
                    )
                    workManager.enqueue(
                        FolderDownloadWorker.getWorkRequest(
                            driveLink = folderDriveLink,
                            tags = rootFolderId?.let { listOf(uniqueFolderWorkName(rootFolderId)) } ?: emptyList(),
                        )
                    ).await()
                } else {
                    CoreLogger.e(
                        tag = logTag,
                        message = "Download is finished but not all files are downloaded",
                    )
                    return Result.failure()
                }
            } else {
                if (hasEnqueuedWorker()) {
                    CoreLogger.d(
                        tag = logTag,
                        message = "Download is finished but not all files are downloaded, enqueued worker will re-check",
                    )
                    return Result.success()
                }
                return if (runAttemptCount < MAX_ATTEMPT_COUNT) {
                    CoreLogger.d(
                        tag = logTag,
                        message = "Download is finished but not all files are downloaded, re-checking ($runAttemptCount)",
                    )
                    Result.retry()
                } else {
                    CoreLogger.d(
                        tag = logTag,
                        message = "Download is finished but not all files are downloaded, too many attempts, giving up",
                    )
                    Result.failure()
                }
            }
        }
        return Result.success()
    }

    private suspend fun areAllDescendantsDownloaded(driveLink: DriveLink.Folder): Boolean =
        (listOf(folderId) + getDescendants(
            folderLink = driveLink.link,
            refresh = false,
        )
            .getOrThrow()
            .filterIsInstance<Link.Folder>()
            .map { folder -> folder.id }
        )
            .map { folderId -> areAllFilesDownloaded(folderId) }
            .all { allDownloaded -> allDownloaded }

    private suspend fun finishedWorkers(): Boolean {
        val works = workManager.getWorkInfosByTagFlow(uniqueFolderWorkName(folderId)).firstOrNull()
        return works == null || works
            .filterNot { workInfo -> workInfo.tags.contains(ROOT_FOLDER_DOWNLOAD_STATE_UPDATE_TAG) }
            .all { workInfo -> workInfo.state.isFinished }
    }

    private suspend fun hasEnqueuedWorker(): Boolean {
        val works = workManager.getWorkInfosByTagFlow(uniqueFolderWorkName(folderId)).firstOrNull()
        return works != null && works
            .filter { workInfo -> workInfo.tags.contains(FOLDER_DOWNLOAD_STATE_UPDATE_TAG) }
            .any { workInfo -> workInfo.state == WorkInfo.State.ENQUEUED }
    }

    companion object {
        private const val MAX_ATTEMPT_COUNT = 10
        private const val FOLDER_DOWNLOAD_STATE_UPDATE_TAG = "folderDownloadStateUpdate"

        const val ROOT_FOLDER_DOWNLOAD_STATE_UPDATE_TAG = "rootFolderDownloadStateUpdate"

        fun getWorkRequest(
            userId: UserId,
            folderId: FolderId,
            isDownloadFinished: Boolean,
            tags: List<String>,
            rootFolderId: FolderId? = null,
        ): OneTimeWorkRequest {
            rootFolderId?.let {
                require(folderId.shareId == rootFolderId.shareId) { "Root folder id must be parent of a folder id" }
            }
            return OneTimeWorkRequest.Builder(FolderDownloadStateUpdateWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, userId.id)
                        .putString(KEY_SHARE_ID, folderId.shareId.id)
                        .putString(KEY_FOLDER_ID, folderId.id)
                        .apply {
                            rootFolderId?.let{
                                putString(KEY_ROOT_FOLDER_ID, rootFolderId.id)
                            }
                        }
                        .putBoolean(KEY_IS_DOWNLOAD_FINISHED, isDownloadFinished)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .addTags(listOf(userId.id, FOLDER_DOWNLOAD_STATE_UPDATE_TAG) + tags)
                .build()
        }
    }
}
