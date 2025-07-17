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
package me.proton.core.drive.photo.data.worker

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
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.photo.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.photo.data.worker.WorkerKeys.KEY_VOLUME_ID
import me.proton.core.drive.photo.domain.usecase.TagsMigrationExtractTagsForVolume
import me.proton.core.drive.volume.domain.entity.VolumeId

@HiltWorker
class TagsMigrationExtractTagsForVolumeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val extractTagsForVolume: TagsMigrationExtractTagsForVolume,
) : CoroutineWorker(
    appContext = appContext,
    params = workerParams,
) {
    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val volumeId = VolumeId(requireNotNull(inputData.getString(KEY_VOLUME_ID)))

    override suspend fun doWork() = extractTagsForVolume(userId, volumeId).fold(
        onSuccess = {
            Result.success()
        },
        onFailure = { error ->
            if (error.isRetryable) {
                Result.retry()
            } else {
                error.log(LogTag.PHOTO, "Cannot extract tags from photos")
                Result.failure()
            }
        }
    )

    companion object {
        fun getWorkRequest(
            userId: UserId,
            volumeId: VolumeId,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(TagsMigrationExtractTagsForVolumeWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    workDataOf(userId, volumeId)
                )
                .addTags(listOf(userId.id) + tags)
                .build()

        internal fun workDataOf(userId: UserId, volumeId: VolumeId) = Data.Builder()
            .putString(KEY_USER_ID, userId.id)
            .putString(KEY_VOLUME_ID, volumeId.id)
            .build()
    }
}
