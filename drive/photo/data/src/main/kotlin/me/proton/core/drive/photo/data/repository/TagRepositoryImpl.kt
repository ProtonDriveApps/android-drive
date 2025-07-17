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

package me.proton.core.drive.photo.data.repository

import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.data.api.PhotoApiDataSource
import me.proton.core.drive.photo.data.api.request.FavoriteRequest
import me.proton.core.drive.photo.data.api.request.PhotoData
import me.proton.core.drive.photo.data.api.request.RelatedPhoto
import me.proton.core.drive.photo.domain.entity.PhotoFavoriteInfo
import me.proton.core.drive.photo.domain.repository.TagRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class TagRepositoryImpl @Inject constructor(
    private val api: PhotoApiDataSource,
) : TagRepository {
    override suspend fun addFavorite(
        volumeId: VolumeId,
        fileId: FileId,
        photoFavoriteInfo: PhotoFavoriteInfo?,
    ) {
        api.addFavorite(
            volumeId, fileId, FavoriteRequest(
                photoFavoriteInfo?.let { info ->
                    PhotoData(
                        hash = info.hash,
                        name = info.name,
                        nameSignatureEmail = info.nameSignatureEmail,
                        nodePassphrase = info.nodePassphrase,
                        contentHash = info.contentHash,
                        nodePassphraseSignature = info.nodePassphraseSignature,
                        signatureEmail = info.signatureEmail,
                        relatedPhotos = info.relatedPhotoFavoriteInfos.map { relatedInfo ->
                            RelatedPhoto(
                                linkId = relatedInfo.linkId.id,
                                hash = relatedInfo.hash,
                                name = relatedInfo.name,
                                nameSignatureEmail = relatedInfo.nameSignatureEmail,
                                nodePassphrase = relatedInfo.nodePassphrase,
                                contentHash = relatedInfo.contentHash,
                                nodePassphraseSignature = relatedInfo.nodePassphraseSignature,
                                signatureEmail = relatedInfo.signatureEmail,
                            )
                        },
                    )
                }
            )
        )
    }

    override suspend fun addTags(volumeId: VolumeId, fileId: FileId, tags: Set<PhotoTag>) {
        api.addTag(volumeId, fileId, tags)
    }

    override suspend fun deleteTags(volumeId: VolumeId, fileId: FileId, tags: Set<PhotoTag>) {
        api.deleteTag(volumeId, fileId, tags)
    }
}
