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
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_FILE_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.drivelink.download.data.worker.WorkerKeys.KEY_VOLUME_ID
import me.proton.core.drive.drivelink.download.domain.usecase.DownloadCleanup
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger

@HiltWorker
class DownloadCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadCleanup: DownloadCleanup,
) : CoroutineWorker(appContext, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val volumeId = VolumeId(requireNotNull(inputData.getString(KEY_VOLUME_ID)))
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)))
    private val linkId: LinkId
        get() {
            inputData.getString(KEY_FILE_ID)?.let { fileId ->
                return FileId(shareId, fileId)
            }
            inputData.getString(KEY_FOLDER_ID)?.let { folderId ->
                return FolderId(shareId, folderId)
            }
            throw IllegalStateException("File or folder ID is required")
        }

    override suspend fun doWork(): Result =
        downloadCleanup(volumeId, linkId)
            .onFailure { error ->
                if (error.cause !is NoSuchElementException) {
                    error.log(logTag, "Download cleanup failed")
                }
            }
            .getOrNull()
            ?.let {
                CoreLogger.d(logTag, "Download cleanup successful")
                Result.success()
            }
            ?: Result.failure()

    private val logTag = "${LogTag.DOWNLOAD}.${linkId.id.logId()}"

    companion object {
        fun getWorkRequest(
            userId: UserId,
            volumeId: VolumeId,
            linkId: LinkId,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(DownloadCleanupWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, userId.id)
                        .putString(KEY_VOLUME_ID, volumeId.id)
                        .putString(KEY_SHARE_ID, linkId.shareId.id)
                        .putString(
                            when (linkId) {
                                is FileId -> KEY_FILE_ID
                                is FolderId -> KEY_FOLDER_ID
                            },
                            linkId.id
                        )
                        .build()
                )
                .addTags(listOf(userId.id) + tags)
                .build()
    }
}
