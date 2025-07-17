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

package me.proton.core.drive.photo.domain.usecase

import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.PHOTO
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetOrCreateClientUid
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.photo.domain.entity.TagsMigrationAnchor
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationStatus
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class TagsMigrationUpdateTagsForVolume @Inject constructor(
    private val tagsMigrationUpdateTags: TagsMigrationUpdateTags,
    private val getTagsMigrationBatchFiles: GetTagsMigrationBatchFiles,
    private val updateTagsMigrationStatus: UpdateTagsMigrationStatus,
    private val getTagsMigrationStatistics: GetTagsMigrationStatistics,
    private val configurationProvider: ConfigurationProvider,
    private val getOrCreateClientUid: GetOrCreateClientUid,
) {
    suspend operator fun invoke(userId: UserId, volumeId: VolumeId) = coRunCatching {
        var files: List<TagsMigrationFile>
        val clientUid = getOrCreateClientUid().getOrThrow()
        do {
            files = getTagsMigrationBatchFiles(
                userId = userId,
                volumeId = volumeId,
                state = TagsMigrationFile.State.EXTRACTED,
                count = configurationProvider.apiPageSize,
            ).getOrThrow()
            if (files.isNotEmpty()) {
                CoreLogger.i(PHOTO, "Updating tags for ${files.size} files")
            }
            files.forEach { file ->
                val fileId = file.fileId
                tagsMigrationUpdateTags(volumeId, fileId)
                    .getOrNull(PHOTO, "Cannot update tags for photo ${fileId.id.logId()}")
            }
            files.minByOrNull { file -> file.captureTime }?.let { oldestFile ->
                val finished = getTagsMigrationStatistics(userId, volumeId).first().isFinished
                CoreLogger.i(
                    PHOTO,
                    "Updating status finished: $finished, for volume: ${volumeId.id.logId()}"
                )
                updateTagsMigrationStatus(
                    userId = userId,
                    volumeId = volumeId,
                    status = TagsMigrationStatus(
                        finished = finished,
                        anchor = TagsMigrationAnchor(
                            lastProcessedLinkId = oldestFile.fileId,
                            lastProcessedCaptureTime = oldestFile.captureTime,
                            currentTimestamp = TimestampS(),
                            clientUid = clientUid,
                        ),
                    ),
                ).getOrThrow()
            }
        } while (files.isNotEmpty())
    }
}
