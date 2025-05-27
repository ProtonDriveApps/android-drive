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

import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.drive.base.domain.extension.onProtonHttpException
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.photo.CreateAddToAlbumInfo
import me.proton.core.drive.documentsprovider.domain.usecase.GetFileIdContentDigestMap
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLinks
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.domain.entity.AddToRemoveFromAlbumResult
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.photo.domain.usecase.GetRelatedPhotoIds
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class AddPhotosToAlbum @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val getRelatedPhotoIds: GetRelatedPhotoIds,
    private val createAddToAlbumInfo: CreateAddToAlbumInfo,
    private val getDriveLinks: GetDriveLinks,
    private val configurationProvider: ConfigurationProvider,
    private val getFileIdContentDigestMap: GetFileIdContentDigestMap,
    private val copyPhoto: CopyPhoto,
) {

    suspend operator fun invoke(
        volumeId: VolumeId,
        albumId: AlbumId,
        photoIds: List<FileId>,
    ): Result<AddToRemoveFromAlbumResult> = coRunCatching {
        val (volumePhotoIds, sharedWithMePhotoIds) = volumeAndSharedWithMeFileIds(
            volumeId = volumeId,
            photoIds = photoIds,
        )
        val addVolumePhotosToAlbumResult = addVolumePhotosToAlbum(
            volumeId = volumeId,
            albumId = albumId,
            photoIds = volumePhotoIds + volumePhotoIds.map { photoId ->
                getRelatedPhotoIds(volumeId, photoId).getOrThrow()
            }.flatten(),
        )
        val addSharedWithMePhotosToAlbumResult = addSharedWithMePhotosToAlbum(
            albumId = albumId,
            photoIds = sharedWithMePhotoIds + sharedWithMePhotoIds.map { photoId ->
                getRelatedPhotoIds(volumeId, photoId).getOrThrow()
            }.flatten(),
        )
        AddToRemoveFromAlbumResult(
            addVolumePhotosToAlbumResult.results + addSharedWithMePhotosToAlbumResult.results
        )
    }

    private suspend fun addVolumePhotosToAlbum(
        volumeId: VolumeId,
        albumId: AlbumId,
        photoIds: List<FileId>,
    ): AddToRemoveFromAlbumResult = albumRepository.addToAlbum(
        volumeId = volumeId,
        albumId = albumId,
        addToAlbumInfos =
        createAddToAlbumInfo(
            photoIds = photoIds,
            albumId = albumId,
            contentDigests = getFileIdContentDigestMap(photoIds.toSet()),
        ).getOrThrow(),
    )

    private suspend fun addSharedWithMePhotosToAlbum(
        albumId: AlbumId,
        photoIds: List<FileId>,
    ): AddToRemoveFromAlbumResult = copyPhoto(
        newParentId = albumId,
        fileIds = photoIds,
        shouldUpdateEvent = false,
    ).map { result ->
        result.fold(
            onFailure = { error ->
                val (code, error) = error.onProtonHttpException { protonData ->
                    protonData.code.toLong() to protonData.error
                } ?: (-1L to "")
                AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error(
                    fileId = "",
                    code = code,
                    error = error,
                )
            },
            onSuccess = { photoId ->
                AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success(photoId.id)
            }
        )
    }.let { AddToRemoveFromAlbumResult(it) }

    private suspend fun volumeAndSharedWithMeFileIds(
        volumeId: VolumeId,
        photoIds: List<FileId>,
    ): Pair<List<FileId>, List<FileId>> {
        val volumePhotoIds = mutableListOf<FileId>()
        val sharedWithMePhotoIds = mutableListOf<FileId>()
        photoIds.chunked(configurationProvider.dbPageSize).forEach { chunkedPhotoIds ->
            getDriveLinks(chunkedPhotoIds).firstOrNull()?.let { photoDriveLinks ->
                photoDriveLinks.forEach { photo ->
                    if (photo.volumeId == volumeId) {
                        volumePhotoIds.add(photo.id as FileId)
                    } else {
                        sharedWithMePhotoIds.add(photo.id as FileId)
                    }
                }
            }
        }
        return volumePhotoIds to sharedWithMePhotoIds
    }
}
