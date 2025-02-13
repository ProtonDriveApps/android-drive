/*
 * Copyright (c) 2023 Proton AG.
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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.extension.toBackupError
import me.proton.core.drive.backup.data.manager.BackupManagerImpl
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.CleanRevisions
import me.proton.core.drive.backup.domain.usecase.HandleBackupError
import me.proton.core.drive.base.data.entity.LoggerLevel.WARNING
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId

@HiltWorker
class BackupCleanRevisionsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val cleanRevisions: CleanRevisions,
    private val configurationProvider: ConfigurationProvider,
    private val handleBackupError: HandleBackupError,
) : CoroutineWorker(context, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)))
    private val folderId = FolderId(shareId, requireNotNull(inputData.getString(KEY_FOLDER_ID)))

    override suspend fun doWork(): Result {
        cleanRevisions(folderId).onFailure { error ->
            val canRetry = runAttemptCount <= configurationProvider.maxApiAutoRetries
            val retryable = error.isRetryable

            return if (canRetry && retryable) {
                error.log(
                    tag = BACKUP,
                    message = "Cannot clean revisions for: ${folderId.id}, will retry",
                    level = WARNING,
                )
                Result.retry()
            } else {
                error.log(
                    tag = BACKUP,
                    message = "Cannot clean revisions for: ${folderId.id} retryable $retryable, max retries reached $canRetry"
                )
                handleBackupError(folderId, error.toBackupError(retryable))
                Result.failure()
            }
        }
        return Result.success()
    }

    companion object {
        fun getWorkRequest(
            folderId: FolderId,
            tags: Collection<String> = emptyList(),
        ) = OneTimeWorkRequest.Builder(BackupCleanRevisionsWorker::class.java)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(workDataOf(folderId))
            .addTags(
                listOf(
                    folderId.userId.id,
                    folderId.id,
                    BackupManagerImpl.TAG
                ) + tags
            )
            .build()

        internal fun workDataOf(
            folderId: FolderId,
        ) = Data.Builder()
            .putString(KEY_USER_ID, folderId.userId.id)
            .putString(KEY_SHARE_ID, folderId.shareId.id)
            .putString(KEY_FOLDER_ID, folderId.id)
            .build()
    }
}
