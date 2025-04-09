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
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.photo.CreateAddToAlbumInfo
import me.proton.core.drive.documentsprovider.domain.usecase.GetContentDigest
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLinks
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.domain.entity.AddToRemoveFromAlbumResult
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class AddPhotosToAlbum @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val createAddToAlbumInfo: CreateAddToAlbumInfo,
    private val getDriveLinks: GetDriveLinks,
    private val configurationProvider: ConfigurationProvider,
    private val getContentDigest: GetContentDigest,
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
            photoIds = volumePhotoIds,
        )
        val addSharedWithMePhotosToAlbumResult = addSharedWithMePhotosToAlbum(
            volumeId = volumeId,
            albumId = albumId,
            photoIds = sharedWithMePhotoIds,
        )
        AddToRemoveFromAlbumResult(
            addVolumePhotosToAlbumResult.results + addSharedWithMePhotosToAlbumResult.results
        )
    }

    private suspend fun addVolumePhotosToAlbum(
        volumeId: VolumeId,
        albumId: AlbumId,
        photoIds: List<FileId>,
    ) = albumRepository.addToAlbum(
        volumeId = volumeId,
        albumId = albumId,
        addToAlbumInfos = photoIds.map { photoId ->
            createAddToAlbumInfo(
                photoId = photoId,
                albumId = albumId,
                contentDigest = getContentDigest(photoId).getOrThrow(),
            ).getOrThrow()
        },
    )

    private suspend fun addSharedWithMePhotosToAlbum(
        volumeId: VolumeId,
        albumId: AlbumId,
        photoIds: List<FileId>,
    ): AddToRemoveFromAlbumResult {
        if (photoIds.isNotEmpty()) {
            TODO("addSharedWithMePhotosToAlbum is not implemented yet")
        }
        return AddToRemoveFromAlbumResult()
    }

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
