/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.photo.data.manager

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.await
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.data.worker.TagsMigrationExtractTagsForVolumeWorker
import me.proton.core.drive.photo.data.worker.TagsMigrationExtractTagsWorker
import me.proton.core.drive.photo.data.worker.TagsMigrationPrepareFileForVolumeWorker
import me.proton.core.drive.photo.data.worker.TagsMigrationPrepareFileWorker
import me.proton.core.drive.photo.data.worker.TagsMigrationUpdateStatusWorker
import me.proton.core.drive.photo.data.worker.TagsMigrationUpdateTagsForVolumeWorker
import me.proton.core.drive.photo.data.worker.TagsMigrationUpdateTagsWorker
import me.proton.core.drive.photo.domain.manager.PhotoTagWorkManager
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class PhotoTagWorkManagerImpl @Inject constructor(
    private val workManager: WorkManager,
) : PhotoTagWorkManager {
    override suspend fun enqueue(userId: UserId, volumeId: VolumeId) {
        workManager.beginUniqueWork(
            "tags-migration-${volumeId.id}",
            ExistingWorkPolicy.KEEP,
            TagsMigrationPrepareFileForVolumeWorker.getWorkRequest(
                userId = userId,
                volumeId = volumeId,
                tags = listOf(volumeId.tagId(userId)),
            )
        ).then(
            TagsMigrationExtractTagsForVolumeWorker.getWorkRequest(
                userId = userId,
                volumeId = volumeId,
                tags = listOf(volumeId.tagId(userId)),
            )
        ).then(
            TagsMigrationUpdateTagsForVolumeWorker.getWorkRequest(
                userId = userId,
                volumeId = volumeId,
                tags = listOf(volumeId.tagId(userId)),
            )
        ).enqueue().await()
    }

    override suspend fun cancel(userId: UserId, volumeId: VolumeId) {
        workManager.cancelAllWorkByTag(volumeId.tagId(userId))
    }

    override suspend fun prepare(volumeId: VolumeId, fileId: FileId) {
        workManager.enqueue(
            TagsMigrationPrepareFileWorker.getWorkRequest(
                volumeId = volumeId,
                fileId = fileId,
                tags = listOf(volumeId.tagId(fileId.userId)),
            )
        ).await()
    }

    override suspend fun tag(volumeId: VolumeId, fileId: FileId, updateStatus: Boolean) {
        workManager.beginUniqueWork(
            "tags-migration-${volumeId.id}-${fileId.id}",
            ExistingWorkPolicy.KEEP,
            TagsMigrationExtractTagsWorker.getWorkRequest(
                volumeId = volumeId,
                fileId = fileId,
                tags = listOf(volumeId.tagId(fileId.userId)),
            )
        ).then(
            TagsMigrationUpdateTagsWorker.getWorkRequest(
                volumeId = volumeId,
                fileId = fileId,
                tags = listOf(volumeId.tagId(fileId.userId)),
            )
        ).run {
            if (updateStatus) {
                then(
                    TagsMigrationUpdateStatusWorker.getWorkRequest(
                        volumeId = volumeId,
                        fileId = fileId,
                        tags = listOf(volumeId.tagId(fileId.userId)),
                    )
                )
            } else {
                this
            }
        }.enqueue().await()
    }

    fun VolumeId.tagId(userId: UserId) = "photo-tag-worker-${userId.id}-$id"
}
