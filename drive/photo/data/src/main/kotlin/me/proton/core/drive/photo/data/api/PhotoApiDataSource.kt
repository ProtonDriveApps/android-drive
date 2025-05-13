/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.photo.data.api

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.data.api.request.AddToAlbumRequest
import me.proton.core.drive.photo.data.api.request.CreateAlbumRequest
import me.proton.core.drive.photo.data.api.request.CreatePhotoRequest
import me.proton.core.drive.photo.data.api.request.FavoriteRequest
import me.proton.core.drive.photo.data.api.request.FindDuplicatesRequest
import me.proton.core.drive.photo.data.api.request.RemoveFromAlbumRequest
import me.proton.core.drive.photo.data.api.request.TagRequest
import me.proton.core.drive.photo.data.api.request.UpdateAlbumRequest
import me.proton.core.drive.photo.data.extension.toAlbumData
import me.proton.core.drive.photo.data.extension.toDtoSort
import me.proton.core.drive.photo.domain.entity.AddToAlbumInfo
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.sorting.data.extension.toDtoDesc
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException
import me.proton.core.util.kotlin.toInt

@Suppress("LongParameterList")
class PhotoApiDataSource(private val apiProvider: ApiProvider) {

    @Throws(ApiException::class)
    suspend fun createPhotoShareWithRootLink(userId: UserId, volumeId: VolumeId, request: CreatePhotoRequest) =
        apiProvider
            .get<PhotoApi>(userId)
            .invoke {
                createPhotoShareWithRootLink(volumeId.id, request)
            }.valueOrThrow.share

    @Throws(ApiException::class)
    suspend fun getPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        sortingDirection: Direction,
        pageSize: Int,
        previousPageLastLinkId: String?,
        minimumCaptureTime: TimestampS,
        tag: Long? = null
    ) =
        apiProvider
            .get<PhotoApi>(userId)
            .invoke {
                getPhotoListings(
                    volumeId = volumeId.id,
                    descending = sortingDirection.toDtoDesc(),
                    pageSize = requireNotNull(takeIf { pageSize in 1..500 }?.let { pageSize }),
                    previousPageLastLinkId = previousPageLastLinkId,
                    minimumCaptureTime = minimumCaptureTime.value,
                    tag = tag,
                )
            }.valueOrThrow.photos

    @Throws(ApiException::class)
    suspend fun findDuplicate(userId: UserId, volumeId: VolumeId, request: FindDuplicatesRequest) =
        apiProvider
            .get<PhotoApi>(userId)
            .invoke {
                findDuplicates(volumeId.id, request)
            }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun createAlbum(userId: UserId, volumeId: VolumeId, request: CreateAlbumRequest) =
        apiProvider
            .get<PhotoApi>(userId)
            .invoke {
                createAlbum(volumeId.id, request)
            }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun updateAlbum(
        userId: UserId,
        volumeId: VolumeId,
        albumId: String,
        request: UpdateAlbumRequest,
    ) = apiProvider
        .get<PhotoApi>(userId)
        .invoke {
            updateAlbum(volumeId.id, albumId, request)
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun getAlbumListings(
        userId: UserId,
        volumeId: VolumeId,
        anchorId: String? = null,
    ) = apiProvider
        .get<PhotoApi>(userId)
        .invoke {
            getAlbumListings(volumeId.id, anchorId)
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun getAlbumSharedWithMeListings(
        userId: UserId,
        anchorId: String? = null,
    ) = apiProvider
        .get<PhotoApi>(userId)
        .invoke {
            getAlbumSharedWithMeListings(anchorId)
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun getAlbumPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        albumId: String,
        anchorId: String?,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction,
        onlyDirectChildren: Boolean = false,
        includeTrashedChildren: Boolean = false,
    ) = apiProvider
        .get<PhotoApi>(userId)
        .invoke {
            getAlbumPhotoListings(
                volumeId = volumeId.id,
                linkId = albumId,
                anchorId = anchorId,
                sort = sortingBy.toDtoSort(),
                descending = sortingDirection.toDtoDesc(),
                onlyDirectChildren = onlyDirectChildren.toInt(),
                includeTrashedChildren = includeTrashedChildren.toInt(),
            )
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun addToAlbum(
        userId: UserId,
        volumeId: VolumeId,
        albumId: String,
        addToAlbumInfos: List<AddToAlbumInfo>,
    ) = apiProvider
        .get<PhotoApi>(userId)
        .invoke {
            addToAlbum(
                volumeId = volumeId.id,
                albumId = albumId,
                request = AddToAlbumRequest(
                    albumData = addToAlbumInfos.toAlbumData(),
                ),
            )
        }

    @Throws(ApiException::class)
    suspend fun removeFromAlbum(
        userId: UserId,
        volumeId: VolumeId,
        albumId: String,
        linkIds: List<String>,
    ) = apiProvider
        .get<PhotoApi>(userId)
        .invoke {
            removeFromAlbum(
                volumeId = volumeId.id,
                albumId = albumId,
                request = RemoveFromAlbumRequest(
                    linkIds = linkIds,
                ),
            )
        }

    @Throws(ApiException::class)
    suspend fun deleteAlbum(
        userId: UserId,
        volumeId: VolumeId,
        albumId: String,
        deleteAlbumPhotos: Boolean,
    ) = apiProvider
        .get<PhotoApi>(userId)
        .invoke {
            deleteAlbum(
                volumeId = volumeId.id,
                albumId = albumId,
                deleteAlbumPhotos = deleteAlbumPhotos.toInt(),
            )
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun addFavorite(
        volumeId: VolumeId,
        fileId: FileId,
        request: FavoriteRequest = FavoriteRequest(),
    ) = apiProvider
        .get<PhotoApi>(fileId.userId)
        .invoke {
            addFavorite(
                volumeId = volumeId.id,
                linkId = fileId.id,
                request = request
            )
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun addTag(
        volumeId: VolumeId,
        fileId: FileId,
        tags: List<PhotoTag>,
    ) = apiProvider
        .get<PhotoApi>(fileId.userId)
        .invoke {
            addTags(
                volumeId = volumeId.id,
                linkId = fileId.id,
                request = TagRequest(
                    tags = tags.map { tag -> tag.value }
                ),
            )
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun deleteTag(
        volumeId: VolumeId,
        fileId: FileId,
        tags: List<PhotoTag>,
    ) = apiProvider
        .get<PhotoApi>(fileId.userId)
        .invoke {
            deleteTags(
                volumeId = volumeId.id,
                linkId = fileId.id,
                request = TagRequest(
                    tags = tags.map { tag -> tag.value }
                ),
            )
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun getPhotoShareMigrationStatus(
        userId: UserId,
    ) = apiProvider
        .get<PhotoApi>(userId)
        .invoke {
            getPhotoShareMigrationStatus()
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun startPhotoShareMigration(
        userId: UserId,
    ) = apiProvider
        .get<PhotoApi>(userId)
        .invoke {
            startPhotoShareMigration()
        }.valueOrThrow
}
