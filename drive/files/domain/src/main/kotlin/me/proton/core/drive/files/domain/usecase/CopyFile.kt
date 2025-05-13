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

package me.proton.core.drive.files.domain.usecase

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.file.CreateCopyInfo
import me.proton.core.drive.link.domain.entity.CopyInfo
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.domain.repository.LinkRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class CopyFile @Inject constructor(
    private val repository: LinkRepository,
    private val createCopyInfo: CreateCopyInfo,
) {

    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        relatedPhotosIds: List<FileId>,
        newVolumeId: VolumeId,
        newParentId: ParentId,
        contentDigestMap: Map<FileId, String?> = emptyMap(),
    ) = coRunCatching {
        val copyInfo = createCopyInfo(
            newVolumeId = newVolumeId,
            newParentId = newParentId,
            fileId = fileId,
            relatedPhotoIds = relatedPhotosIds,
            contentDigestMap = contentDigestMap,
        ).getOrThrow()
        invoke(
            volumeId = volumeId,
            fileId = fileId,
            newParentId = newParentId,
            copyInfo = copyInfo,
        ).getOrThrow()
    }

    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        newParentId: ParentId,
        copyInfo: CopyInfo
    )  = coRunCatching {
        repository.copyFile(
            volumeId = volumeId,
            fileId = fileId,
            targetParentId = newParentId,
            copyInfo = copyInfo
        )
    }
}
