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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.PHOTO
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class TagsMigrationExtractTagsForVolume @Inject constructor(
    private val tagsMigrationExtractTags: TagsMigrationExtractTags,
    private val getTagsMigrationBatchFiles: GetTagsMigrationBatchFiles,
    private val configurationProvider: ConfigurationProvider
) {
    suspend operator fun invoke(userId: UserId, volumeId: VolumeId) = coRunCatching {
        var files: List<TagsMigrationFile>
        do {
            files = getTagsMigrationBatchFiles(
                userId = userId,
                volumeId = volumeId,
                state = TagsMigrationFile.State.DOWNLOADED,
                count = configurationProvider.dbPageSize,
            ).getOrThrow()
            if (files.isNotEmpty()) {
                CoreLogger.i(PHOTO, "Extracting tags for ${files.size} files")
            }
            files.forEach { file ->
                val fileId = file.fileId
                tagsMigrationExtractTags(volumeId, fileId)
                    .getOrNull(PHOTO, "Cannot extract tags from photo ${fileId.id.logId()}")
            }
        } while (files.isNotEmpty())
    }
}
