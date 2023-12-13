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

package me.proton.core.drive.photo.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.ClientUid
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.photo.data.api.PhotoApiDataSource
import me.proton.core.drive.photo.data.api.entity.PhotoListingDto
import me.proton.core.drive.photo.data.api.request.FindDuplicatesRequest
import me.proton.core.drive.photo.data.db.PhotoDatabase
import me.proton.core.drive.photo.data.extension.toCreatePhotoRequest
import me.proton.core.drive.photo.data.extension.toPhotoDuplicate
import me.proton.core.drive.photo.data.extension.toPhotoListing
import me.proton.core.drive.photo.data.extension.toPhotoListingEntity
import me.proton.core.drive.photo.domain.entity.PhotoDuplicate
import me.proton.core.drive.photo.domain.entity.PhotoInfo
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

@Suppress("LongParameterList")
class PhotoRepositoryImpl @Inject constructor(
    private val api: PhotoApiDataSource,
    private val db: PhotoDatabase,
) : PhotoRepository {

    override suspend fun createPhotoShareWithRootLink(userId: UserId, photoInfo: PhotoInfo): Pair<String, String> =
        with (api.createPhotoShareWithRootLink(userId, photoInfo.volumeId, photoInfo.toCreatePhotoRequest())) {
            shareId to linkId
        }

    override fun getPhotoListingCount(userId: UserId, volumeId: VolumeId): Flow<Int> =
        db.photoListingDao.getPhotoListingCount(userId, volumeId.id)

    override suspend fun fetchPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        shareId: ShareId,
        pageSize: Int,
        previousPageLastLinkId: String?,
        minimumCaptureTime: TimestampS,
        sortingDirection: Direction,
    ): Pair<List<PhotoListing>, SaveAction> =
        fetchPhotoListingDtos(
            userId = userId,
            volumeId = volumeId,
            sortingDirection = sortingDirection,
            pageSize = pageSize,
            previousPageLastLinkId = previousPageLastLinkId,
            minimumCaptureTime = minimumCaptureTime,
        ).map { photoListingDto ->
            photoListingDto.toPhotoListing(shareId)
        }.let { photoListings ->
            photoListings to SaveAction {
                db.photoListingDao.insertOrUpdate(
                    *photoListings.map { photoListing ->
                        photoListing.toPhotoListingEntity(volumeId)
                    }.toTypedArray()
                )
            }
        }

    override suspend fun fetchAndStorePhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        shareId: ShareId,
        pageSize: Int,
        previousPageLastLinkId: String?,
        minimumCaptureTime: TimestampS,
        sortingDirection: Direction,
    ): List<PhotoListing> {
        val photoListingDtos = fetchPhotoListingDtos(
            userId = userId,
            volumeId = volumeId,
            sortingDirection = sortingDirection,
            pageSize = pageSize,
            previousPageLastLinkId = previousPageLastLinkId,
            minimumCaptureTime = minimumCaptureTime,
        )
        db.photoListingDao.insertOrUpdate(
            *photoListingDtos.map { photoListingDto ->
                photoListingDto.toPhotoListingEntity(volumeId, shareId)
            }.toTypedArray()
        )
        return photoListingDtos.map { photoListingDto -> photoListingDto.toPhotoListing(shareId) }
    }

    override suspend fun getPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        fromIndex: Int,
        count: Int,
        sortingDirection: Direction,
    ): List<PhotoListing> =
        db.photoListingDao.getPhotoListings(
            userId = userId,
            volumeId = volumeId.id,
            direction = sortingDirection,
            limit = count,
            offset = fromIndex,
        ).map { photoListingEntity -> photoListingEntity.toPhotoListing() }

    override suspend fun findDuplicates(
        userId: UserId,
        volumeId: VolumeId,
        parentId: FolderId,
        nameHashes: List<String>,
        clientUids: List<ClientUid>,
    ): List<PhotoDuplicate> =
        api.findDuplicate(
            userId = userId,
            volumeId = volumeId,
            request = FindDuplicatesRequest(
                nameHashes = nameHashes,
                clientUids = clientUids,
            )
        ).duplicates.map { duplicate ->
            duplicate.toPhotoDuplicate(parentId = parentId)
        }

    override fun getPhotoListingsFlow(
        userId: UserId,
        volumeId: VolumeId,
        fromIndex: Int,
        count: Int,
        sortingDirection: Direction
    ): Flow<Result<List<PhotoListing>>> =
        db.photoListingDao.getPhotoListingsFlow(
            userId = userId,
            volumeId = volumeId.id,
            direction = sortingDirection,
            limit = count,
            offset = fromIndex,
        ).map { entities ->
            coRunCatching {
                entities.map { photoListingEntity -> photoListingEntity.toPhotoListing() }
            }
        }

    private suspend fun fetchPhotoListingDtos(
        userId: UserId,
        volumeId: VolumeId,
        pageSize: Int,
        previousPageLastLinkId: String?,
        minimumCaptureTime: TimestampS,
        sortingDirection: Direction,
    ): List<PhotoListingDto> =
        api.getPhotoListings(
            userId = userId,
            volumeId = volumeId,
            sortingDirection = sortingDirection,
            pageSize = pageSize,
            previousPageLastLinkId = previousPageLastLinkId,
            minimumCaptureTime = minimumCaptureTime,
        )

    override suspend fun delete(linkIds: List<LinkId>) {
        db.inTransaction {
            linkIds
                .groupBy({ link -> link.shareId }) { link -> link.id }
                .forEach { (shareId, linkIds) ->
                    db.photoListingDao.delete(shareId.userId, shareId.id, linkIds)
                }
        }
    }

    override suspend fun deleteAll(userId: UserId, volumeId: VolumeId) =
        db.photoListingDao.deleteAll(userId, volumeId.id)

    override suspend fun insertOrIgnore(volumeId: VolumeId, photoListings: List<PhotoListing>) {
        db.photoListingDao.insertOrIgnore(
            *photoListings.map { photoListing -> photoListing.toPhotoListingEntity(volumeId) }.toTypedArray()
        )
    }
}
