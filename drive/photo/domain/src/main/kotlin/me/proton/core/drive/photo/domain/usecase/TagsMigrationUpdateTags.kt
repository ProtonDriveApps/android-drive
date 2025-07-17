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

import me.proton.core.drive.base.domain.log.LogTag.PHOTO
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.IDLE
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.UPDATED
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class TagsMigrationUpdateTags @Inject constructor(
    private val updateTagsMigrationFileState: UpdateTagsMigrationFileState,
    private val getTagsMigrationFileTags: GetTagsMigrationFileTags,
    private val addPhotoTag: AddPhotoTag,
) {
    suspend operator fun invoke(volumeId: VolumeId, fileId: FileId) = coRunCatching {
        val tags = getTagsMigrationFileTags(volumeId, fileId).getOrThrow()
        if (tags.isEmpty()) {
            CoreLogger.d(PHOTO, "No tag found for photo ${fileId.id.logId()}")
        } else {
            addPhotoTag(volumeId, fileId, tags).getOrThrow()
        }
        updateTagsMigrationFileState(volumeId, fileId, UPDATED).getOrThrow()
    }.recoverCatching { error ->
        updateTagsMigrationFileState(volumeId, fileId, IDLE)
        throw error
    }
}
