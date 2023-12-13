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
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.extension.toBackupError
import me.proton.core.drive.backup.data.manager.BackupManagerImpl
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_FILE_URI
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.MarkAsCompleted
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag

@HiltWorker
class BackupClearFileWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val markAsCompleted: MarkAsCompleted,
    private val addBackupError: AddBackupError,
) : CoroutineWorker(context, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val uriString = requireNotNull(inputData.getString(KEY_FILE_URI))

    override suspend fun doWork(): Result {
        markAsCompleted(userId, uriString).onFailure { error ->
            error.log(LogTag.BACKUP, "Cannot mark file as completed with uri: $uriString")
            addBackupError(userId, error.toBackupError())
            return Result.failure()
        }
        return Result.success()
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            uriString: String,
            tags: Collection<String> = emptyList(),
        ): OneTimeWorkRequest {
            return OneTimeWorkRequest.Builder(BackupClearFileWorker::class.java)
                .setInputData(workDataOf(userId, uriString))
                .addTags(listOf(userId.id, BackupManagerImpl.TAG) + tags)
                .build()
        }

        internal fun workDataOf(
            userId: UserId,
            uriString: String,
        ) = Data.Builder()
            .putString(KEY_USER_ID, userId.id)
            .putString(KEY_FILE_URI, uriString)
            .build()
    }
}