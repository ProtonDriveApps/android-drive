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

package me.proton.core.drive.drivelink.photo.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.folder.CreateFolderInfo
import me.proton.core.drive.drivelink.photo.domain.extension.toAlbumInfo
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import javax.inject.Inject

class CreateAlbum @Inject constructor(
    private val createFolderInfo: CreateFolderInfo,
    private val getPhotoShare: GetPhotoShare,
    private val getLink: GetLink,
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(
        userId: UserId,
        albumName: String,
        isLocked: Boolean,
    ): Result<AlbumId> = coRunCatching {
        val photoShare = getPhotoShare(userId).toResult().getOrThrow()
        val photoShareRootLink = getLink(photoShare.rootFolderId).toResult().getOrThrow()
        val (_, folderInfo) = createFolderInfo(photoShareRootLink, albumName).getOrThrow()
        AlbumId(
            shareId = photoShare.id,
            id = albumRepository.createAlbum(
                userId = userId,
                volumeId = photoShare.volumeId,
                albumInfo = folderInfo.toAlbumInfo(isLocked),
            )
        )
    }
}
