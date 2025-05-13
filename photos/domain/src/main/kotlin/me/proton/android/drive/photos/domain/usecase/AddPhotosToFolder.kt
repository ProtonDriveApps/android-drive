/*
 * Copyright (c) 2025 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.domain.usecase

import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.extension.onProtonHttpException
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.file.CreateCopyInfo
import me.proton.core.drive.documentsprovider.domain.usecase.GetContentDigest
import me.proton.core.drive.drivelink.photo.domain.usecase.CopyPhoto
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.photo.domain.entity.AddToRemoveFromAlbumResult
import me.proton.core.drive.photo.domain.usecase.GetRelatedPhotoIds
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class AddPhotosToFolder @Inject constructor(
    private val copyPhoto: CopyPhoto,
    private val configurationProvider: ConfigurationProvider,
    private val createCopyInfo: CreateCopyInfo,
    private val getContentDigest: GetContentDigest,
    private val findAndCheckDuplicates: FindAndCheckDuplicates,
    private val getRelatedPhotoIds: GetRelatedPhotoIds,
    private val getShare: GetShare,
) {

    suspend operator fun invoke(
        photoIds: List<FileId>,
        newVolumeId: VolumeId,
        folderId: FolderId,
        albumId: AlbumId,
    ): Result<AddToRemoveFromAlbumResult> = coRunCatching {
        val limit = configurationProvider.savePhotoToStreamLimit
        check(photoIds.count() < limit) { "Cannot save more than $limit photos" }
        addSharedPhotosToStream(
            volumeId = newVolumeId,
            folderId = folderId,
            photoIds = photoIds,
            albumId = albumId,
        )
    }

    private suspend fun addSharedPhotosToStream(
        volumeId: VolumeId,
        folderId: FolderId,
        photoIds: List<FileId>,
        albumId: AlbumId,
    ): AddToRemoveFromAlbumResult {
        val albumVolumeId = getShare(albumId.shareId).toResult().getOrThrow().volumeId
        val photoIdAndCopyInfosMap = photoIds.associateWith { photoId ->
            val relatedPhotoIds = getRelatedPhotoIds(albumVolumeId, albumId, photoId).getOrThrow()
            createCopyInfo(
                newVolumeId = volumeId,
                newParentId = folderId,
                fileId = photoId,
                relatedPhotoIds = relatedPhotoIds,
                contentDigestMap = (listOf(photoId) + relatedPhotoIds).associateWith { id ->
                    getContentDigest(id).getOrNull()
                }
            ).getOrThrow()
        }

        val duplicateFileIds = findAndCheckDuplicates(
            newParentId = folderId,
            copyInfos = photoIdAndCopyInfosMap
        ).getOrThrow()

        return photoIdAndCopyInfosMap
            .map { (photoId, copyInfo) ->
                if (photoId in duplicateFileIds) {
                    AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error(
                        photoId.id, ProtonApiCode.ALREADY_EXISTS.toLong(), null
                    )
                } else {
                    val error = copyPhoto(
                        newParentId = folderId,
                        fileId = photoId,
                        copyInfo = copyInfo,
                        shouldUpdateEvent = false,
                    ).exceptionOrNull()
                    if (error == null) {
                        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success(photoId.id)
                    } else {
                        val (code, error) = error.onProtonHttpException { protonData ->
                            protonData.code.toLong() to protonData.error
                        } ?: (-1L to "")
                        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error(
                            fileId = photoId.id,
                            code = code,
                            error = error,
                        )
                    }
                }
            }.takeIfNotEmpty()?.let {
                AddToRemoveFromAlbumResult(it)
            } ?: AddToRemoveFromAlbumResult()
    }
}
