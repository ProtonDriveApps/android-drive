/*
 * Copyright (c) 2022-2024 Proton AG.
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
package me.proton.core.drive.backup.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.backup.domain.usecase.PostBackupNotification
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.util.kotlin.CoreLogger

@HiltWorker
class BackupNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val postBackupNotification: PostBackupNotification,
) : CoroutineWorker(appContext, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))

    override suspend fun doWork(): Result {
        inputData.folderId
            .onFailure { error ->
                error.log(BACKUP, "Failed to get FolderId")
            }
            .onSuccess { folderId ->
                CoreLogger.d(BACKUP, "Preparing notification for ${folderId.id.logId()}")
                postBackupNotification(folderId).onFailure { error ->
                    error.log(BACKUP, "Failed to update notification")
                }
            }
        return Result.success()
    }

    private val Data.folderId: kotlin.Result<FolderId> get() = coRunCatching {
        FolderId(
            shareId = ShareId(userId, requireNotNull(getString(KEY_SHARE_ID))),
            id = requireNotNull(getString(KEY_FOLDER_ID)),
        )
    }

    companion object {
        fun getWorkRequest(
            folderId: FolderId,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(BackupNotificationWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, folderId.userId.id)
                        .putString(KEY_SHARE_ID, folderId.shareId.id)
                        .putString(KEY_FOLDER_ID, folderId.id)
                        .build()
                )
                .addTags(
                    listOf(
                        folderId.userId.id,
                        folderId.id,
                    ) + tags
                )
                .build()
    }
}
