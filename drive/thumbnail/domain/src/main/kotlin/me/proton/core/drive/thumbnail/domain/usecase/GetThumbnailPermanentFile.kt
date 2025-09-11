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

package me.proton.core.drive.thumbnail.domain.usecase

import me.proton.core.drive.base.domain.extension.changeParent
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.extension.getThumbnailIds
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.volume.domain.entity.VolumeId
import java.io.File
import javax.inject.Inject

class GetThumbnailPermanentFile @Inject constructor(
    private val getLink: GetLink,
    private val getThumbnailBlock: GetThumbnailBlock,
    private val getPermanentFolder: GetPermanentFolder,
) {
    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
    ) = coRunCatching {
        getLink(fileId)
            .toResult()
            .getOrThrow()
            .let { link ->
                link.getThumbnailIds(volumeId).forEach { thumbnailId ->
                    getThumbnailBlock(
                        fileId = fileId,
                        volumeId = volumeId,
                        revisionId = revisionId,
                        thumbnailId = thumbnailId,
                    )
                        .getOrThrow()
                        .let { block ->
                            File(block.url)
                                .changeParent(
                                    getPermanentFolder(fileId.userId, volumeId.id, revisionId)
                                )
                        }
                }
            }
    }
}
