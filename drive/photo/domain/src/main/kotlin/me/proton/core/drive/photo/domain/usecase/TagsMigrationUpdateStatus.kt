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
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.log.LogTag.PHOTO
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.usecase.GetOrCreateClientUid
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.entity.TagsMigrationAnchor
import me.proton.core.drive.photo.domain.entity.TagsMigrationStatus
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class TagsMigrationUpdateStatus @Inject constructor(
    private val updateStatus: UpdateTagsMigrationStatus,
    private val getTagsMigrationFile: GetTagsMigrationFile,
    private val getTagsMigrationStatistics: GetTagsMigrationStatistics,
    private val getOrCreateClientUid: GetOrCreateClientUid,
) {
    suspend operator fun invoke(volumeId: VolumeId, fileId: FileId) = coRunCatching {
        val file = getTagsMigrationFile(
            volumeId = volumeId,
            fileId = fileId,
        ).getOrThrow()
        checkNotNull(file) { "Cannot find tags migration file: ${fileId.id.logId()}" }
        val finished = getTagsMigrationStatistics(fileId.userId, volumeId).first().isFinished
        updateStatus(
            userId = fileId.userId,
            volumeId = volumeId,
            status = TagsMigrationStatus(
                finished = finished,
                anchor = TagsMigrationAnchor(
                    lastProcessedLinkId = fileId,
                    lastProcessedCaptureTime = file.captureTime,
                    currentTimestamp = TimestampS(),
                    clientUid = getOrCreateClientUid().getOrThrow(),
                )
            ),
        ).getOrThrow()
        if (finished) {
            CoreLogger.i(PHOTO, "Migration finished for volume: ${volumeId.id.logId()}")
        }
    }
}
