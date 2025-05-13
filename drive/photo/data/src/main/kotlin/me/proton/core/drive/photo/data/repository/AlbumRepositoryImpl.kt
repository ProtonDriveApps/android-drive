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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.repository.BaseRepository
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.data.api.PhotoApiDataSource
import me.proton.core.drive.photo.data.api.response.AddToRemoveFromAlbumResponse
import me.proton.core.drive.photo.data.api.response.GetAlbumPhotoListingResponse
import me.proton.core.drive.photo.data.db.PhotoDatabase
import me.proton.core.drive.photo.data.db.entity.AlbumListingEntity
import me.proton.core.drive.photo.data.extension.toAddToRemoveFromAlbumResult
import me.proton.core.drive.photo.data.extension.toAlbumListing
import me.proton.core.drive.photo.data.extension.toAlbumListingEntity
import me.proton.core.drive.photo.data.extension.toAlbumPhotoListing
import me.proton.core.drive.photo.data.extension.toAlbumPhotoListingEntity
import me.proton.core.drive.photo.data.extension.toCreateAlbumRequest
import me.proton.core.drive.photo.data.extension.toEntity
import me.proton.core.drive.photo.data.extension.toUpdateAlbumRequest
import me.proton.core.drive.photo.domain.entity.AddToAlbumInfo
import me.proton.core.drive.photo.domain.entity.AddToRemoveFromAlbumResult
import me.proton.core.drive.photo.domain.entity.AlbumInfo
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.photo.domain.entity.AlbumPhotoListingList
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.photo.domain.entity.RelatedPhoto
import me.proton.core.drive.photo.domain.entity.UpdateAlbumInfo
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class AlbumRepositoryImpl @Inject constructor(
    private val api: PhotoApiDataSource,
    private val db: PhotoDatabase,
    private val baseRepository: BaseRepository,
    private val configurationProvider: ConfigurationProvider,
    private val getShare: GetShare,
) : AlbumRepository {

    override suspend fun createAlbum(userId: UserId, volumeId: VolumeId, albumInfo: AlbumInfo) =
        api.createAlbum(userId, volumeId, albumInfo.toCreateAlbumRequest()).album.link.linkId

    override suspend fun updateAlbum(
        volumeId: VolumeId,
        albumId: AlbumId,
        updateAlbumInfo: UpdateAlbumInfo,
    ) {
        api.updateAlbum(
            userId = albumId.userId,
            volumeId = volumeId,
            albumId = albumId.id,
            request = updateAlbumInfo.toUpdateAlbumRequest(),
        )
    }

    override suspend fun fetchAndStoreAllAlbumListings(
        userId: UserId,
        volumeId: VolumeId,
        shareId: ShareId,
    ): List<AlbumListing> {
        var anchorId: String? = null
        val albumListingEntities: MutableList<AlbumListingEntity> = mutableListOf()
        while (true) {
            val response = api.getAlbumListings(userId, volumeId, anchorId)
            albumListingEntities.addAll(
                response.albums.map { albumListingsDto ->
                    albumListingsDto.toAlbumListingEntity(shareId)
                }
            )
            if (response.more && response.anchorId != null) {
                anchorId = response.anchorId
            } else {
                break
            }
        }
        anchorId = null
        while (true) {
            val response = api.getAlbumSharedWithMeListings(userId, anchorId)
            albumListingEntities.addAll(
                response.albums.mapNotNull { albumListingsDto ->
                    albumListingsDto.shareId?.let { shareId ->
                        val albumShareId = ShareId(userId, shareId)
                        getShare(albumShareId).toResult().getOrThrow()
                        albumListingsDto.toAlbumListingEntity(albumShareId)
                    }
                }
            )
            if (response.more && response.anchorId != null) {
                anchorId = response.anchorId
            } else {
                break
            }
        }
        db.inTransaction {
            db.albumListingDao.deleteAll(userId)
            db.albumListingDao.insertOrIgnore(
                *albumListingEntities.toTypedArray()
            )
            baseRepository.setLastFetch(userId, getAlbumListingsUrl(volumeId), TimestampMs())
        }
        return albumListingEntities.map { albumListingEntity -> albumListingEntity.toAlbumListing() }
    }

    override suspend fun deleteAll(userId: UserId, volumeId: VolumeId) =
        db.albumListingDao.deleteAll(userId, volumeId.id)

    override suspend fun getAlbumListings(
        userId: UserId,
        fromIndex: Int,
        count: Int,
        sortingDirection: Direction,
    ): List<AlbumListing> =
        db.albumListingDao.getAlbumListings(
            userId = userId,
            direction = sortingDirection,
            limit = count,
            offset = fromIndex,
        ).map { albumListingEntity -> albumListingEntity.toAlbumListing() }

    override fun getAlbumListingsFlow(
        userId: UserId,
        fromIndex: Int,
        count: Int,
        sortingDirection: Direction
    ): Flow<Result<List<AlbumListing>>> =
        db.albumListingDao.getAlbumListingsFlow(
            userId = userId,
            direction = sortingDirection,
            limit = count,
            offset = fromIndex,
        ).map { entities ->
            coRunCatching {
                entities.map { albumListingEntity -> albumListingEntity.toAlbumListing() }
            }
        }

    override suspend fun insertOrUpdateAlbumListings(
        albumListings: List<AlbumListing>
    ) {
        db.albumListingDao.insertOrUpdate(
            *albumListings.map { albumListing ->
                albumListing.toAlbumListingEntity()
            }.toTypedArray()
        )
    }

    override suspend fun deleteAlbumListings(albumIds: List<AlbumId>) {
        db.inTransaction {
            albumIds
                .groupBy({ albumId -> albumId.shareId }) { albumId -> albumId.id }
                .forEach { (shareId, albumIds) ->
                    db.albumListingDao.delete(shareId.userId, shareId.id, albumIds)
                }
        }
    }

    override suspend fun deleteAlbumPhotoListing(linkIds: List<LinkId>) {
        db.inTransaction {
            linkIds
                .groupBy({ link -> link.shareId }) { link -> link.id }
                .forEach { (shareId, linkIds) ->
                    db.albumPhotoListingDao.delete(shareId.userId, shareId.id, linkIds)
                }
        }
    }

    override suspend fun fetchAlbumPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        albumId: AlbumId,
        anchorId: String?,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction,
        onlyDirectChildren: Boolean,
        includeTrashedChildren: Boolean,
    ): Pair<AlbumPhotoListingList, SaveAction> =
        fetchAlbumPhotoListingDtos(
            userId = userId,
            volumeId = volumeId,
            albumId = albumId,
            anchorId = anchorId,
            sortingBy = sortingBy,
            sortingDirection = sortingDirection,
            onlyDirectChildren = onlyDirectChildren,
            includeTrashedChildren = includeTrashedChildren,
        ).let { response ->
            val albumPhotoListings = response.photos.map { albumPhotoListingDto ->
                albumPhotoListingDto.toAlbumPhotoListing(albumId.shareId, albumId)
            }
            AlbumPhotoListingList(
                list = albumPhotoListings,
                anchorId = response.anchorId,
                hasMore = response.hasMore,
            ) to SaveAction {
                val map = albumPhotoListings.associate { albumPhotoListing ->
                    albumPhotoListing.toAlbumPhotoListingEntity(volumeId)
                }
                db.inTransaction {
                    db.albumPhotoListingDao.insertOrUpdate(*map.keys.toTypedArray())
                    db.albumRelatedPhotoDao.insertOrUpdate(*map.values.flatten().toTypedArray())
                }
            }
        }

    override suspend fun fetchAndStoreAlbumPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        shareId: ShareId,
        albumId: AlbumId,
        anchorId: String?,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction,
        onlyDirectChildren: Boolean,
        includeTrashedChildren: Boolean,
    ): List<PhotoListing.Album> {
        val albumPhotoListingDtos = fetchAlbumPhotoListingDtos(
            userId = userId,
            volumeId = volumeId,
            albumId = albumId,
            anchorId = anchorId,
            sortingBy = sortingBy,
            sortingDirection = sortingDirection,
            onlyDirectChildren = onlyDirectChildren,
            includeTrashedChildren = includeTrashedChildren,
        ).photos
        val map = albumPhotoListingDtos.associate { albumPhotoListingDto ->
            albumPhotoListingDto.toAlbumPhotoListingEntity(volumeId, shareId, albumId)
        }
        db.inTransaction {
            db.albumPhotoListingDao.insertOrUpdate(*map.keys.toTypedArray())
            db.albumRelatedPhotoDao.insertOrUpdate(*map.values.flatten().toTypedArray())
        }
        return albumPhotoListingDtos.map { albumPhotoListingDto ->
            albumPhotoListingDto.toAlbumPhotoListing(shareId, albumId)
        }
    }

    override suspend fun getAlbumPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        albumId: AlbumId,
        fromIndex: Int,
        count: Int,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction
    ): List<PhotoListing.Album> =
        db.albumPhotoListingDao.getAlbumPhotoListings(
            userId = userId,
            volumeId = volumeId.id,
            albumId = albumId.id,
            sortingBy = sortingBy,
            direction = sortingDirection,
            limit = count,
            offset = fromIndex,
        ).map { albumPhotoListingEntity -> albumPhotoListingEntity.toAlbumPhotoListing() }

    override fun getAlbumPhotoListingsFlow(
        userId: UserId,
        volumeId: VolumeId,
        albumId: AlbumId,
        fromIndex: Int,
        count: Int,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction
    ): Flow<Result<List<PhotoListing.Album>>> =
        db.albumPhotoListingDao.getAlbumPhotoListingsFlow(
            userId = userId,
            volumeId = volumeId.id,
            albumId = albumId.id,
            sortingBy = sortingBy,
            direction = sortingDirection,
            limit = count,
            offset = fromIndex,
        ).map { entities ->
            coRunCatching {
                entities.map { albumPhotoListingEntity ->
                    albumPhotoListingEntity.toAlbumPhotoListing()
                }
            }
        }

    override fun getAlbumPhotoListingCount(
        userId: UserId,
        volumeId: VolumeId,
        albumId: AlbumId,
    ): Flow<Int> =
        db.albumPhotoListingDao.getAlbumPhotoListingCount(userId, volumeId.id, albumId.id)

    override suspend fun deleteAlbumPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        albumId: AlbumId,
        fileIds: Set<FileId>,
    ) =
        db.albumPhotoListingDao.delete(
            userId = userId,
            volumeId = volumeId.id,
            albumId = albumId.id,
            linkIds = fileIds.map { fileId -> fileId.id },
        )

    override suspend fun deleteAllAlbumPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        albumId: AlbumId
    ) = db.albumPhotoListingDao.deleteAll(userId, volumeId.id, albumId.id)

    override suspend fun insertOrIgnoreAlbumPhotoListing(
        volumeId: VolumeId,
        photoListings: List<PhotoListing.Album>,
    ) {
        val map = photoListings.associate { photoListing ->
            photoListing.toAlbumPhotoListingEntity(volumeId)
        }
        db.inTransaction {
            db.albumPhotoListingDao.insertOrIgnore(*map.keys.toTypedArray())
            db.albumRelatedPhotoDao.insertOrIgnore(*map.values.flatten().toTypedArray())
        }
    }

    override suspend fun addToAlbum(
        volumeId: VolumeId,
        albumId: AlbumId,
        addToAlbumInfos: List<AddToAlbumInfo>,
    ): AddToRemoveFromAlbumResult {
        val responses: MutableList<AddToRemoveFromAlbumResponse.Responses> = mutableListOf()
        val mutex = Mutex()
        withContext(Dispatchers.IO) {
            addToAlbumInfos
                .chunked(configurationProvider.addToRemoveFromAlbumMaxApiDataSize)
                .map { chunk ->
                    async {
                        val response = api.addToAlbum(
                            userId = albumId.userId,
                            volumeId = volumeId,
                            albumId = albumId.id,
                            addToAlbumInfos = chunk,
                        ).valueOrThrow
                        mutex.withLock {
                            responses.addAll(response.responses)
                        }
                    }
                }
                .awaitAll()
        }
        return responses.toAddToRemoveFromAlbumResult()
    }

    override suspend fun removeFromAlbum(
        volumeId: VolumeId,
        albumId: AlbumId,
        linkIds: List<FileId>
    ): AddToRemoveFromAlbumResult {
        val responses: MutableList<AddToRemoveFromAlbumResponse.Responses> = mutableListOf()
        linkIds.chunked(configurationProvider.addToRemoveFromAlbumMaxApiDataSize).forEach { chunk ->
            val response = api.removeFromAlbum(
                userId = albumId.userId,
                volumeId = volumeId,
                albumId = albumId.id,
                linkIds = linkIds.map { linkId -> linkId.id },
            ).valueOrThrow
            responses.addAll(response.responses)
        }
        return responses.toAddToRemoveFromAlbumResult()
    }

    override suspend fun deleteAlbum(
        volumeId: VolumeId,
        albumId: AlbumId,
        deleteAlbumPhotos: Boolean,
    ) {
        api.deleteAlbum(
            userId = albumId.userId,
            volumeId = volumeId,
            albumId = albumId.id,
            deleteAlbumPhotos = deleteAlbumPhotos,
        )
    }

    private suspend fun fetchAlbumPhotoListingDtos(
        userId: UserId,
        volumeId: VolumeId,
        albumId: AlbumId,
        anchorId: String?,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction,
        onlyDirectChildren: Boolean = false,
        includeTrashedChildren: Boolean = false,
    ): GetAlbumPhotoListingResponse =
        api.getAlbumPhotoListings(
            userId = userId,
            volumeId = volumeId,
            albumId = albumId.id,
            anchorId = anchorId,
            sortingBy = sortingBy,
            sortingDirection = sortingDirection,
            onlyDirectChildren = onlyDirectChildren,
            includeTrashedChildren = includeTrashedChildren,
        )

    override suspend fun getRelatedPhotos(
        volumeId: VolumeId,
        albumId: AlbumId,
        mainFileId: FileId,
        fromIndex: Int,
        count: Int,
    ): List<RelatedPhoto> = db.albumRelatedPhotoDao.getRelatedPhotos(
        userId = mainFileId.userId,
        volumeId = volumeId.id,
        albumId = albumId.id,
        mainLinkId = mainFileId.id,
        limit = count,
        offset = fromIndex,
    ).map { it.toEntity() }
}
