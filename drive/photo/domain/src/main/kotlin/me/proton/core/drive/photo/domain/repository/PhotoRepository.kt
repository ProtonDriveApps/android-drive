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

package me.proton.core.drive.photo.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.ClientUid
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.entity.PhotoDuplicate
import me.proton.core.drive.photo.domain.entity.PhotoInfo
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.photo.domain.entity.RelatedPhoto
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.volume.domain.entity.VolumeId

@Suppress("LongParameterList")
interface PhotoRepository {

    suspend fun createPhotoShareWithRootLink(userId: UserId, photoInfo: PhotoInfo): Pair<String, String>

    fun getPhotoListingCount(
        userId: UserId,
        volumeId: VolumeId,
        tag: PhotoTag? = null,
    ): Flow<Int>

    suspend fun fetchPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        shareId: ShareId,
        pageSize: Int,
        previousPageLastLinkId: String? = null,
        minimumCaptureTime: TimestampS = TimestampS(0),
        sortingDirection: Direction = Direction.DESCENDING,
        tag: PhotoTag? = null,
    ): Pair<List<PhotoListing>, SaveAction>

    suspend fun fetchAndStorePhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        shareId: ShareId,
        pageSize: Int,
        previousPageLastLinkId: String? = null,
        minimumCaptureTime: TimestampS = TimestampS(0),
        sortingDirection: Direction = Direction.DESCENDING,
        tag: PhotoTag? = null,
    ): List<PhotoListing>

    suspend fun getPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        fromIndex: Int,
        count: Int,
        sortingDirection: Direction = Direction.DESCENDING,
        tag: PhotoTag? = null,
    ): List<PhotoListing>

    suspend fun findDuplicates(
        userId: UserId,
        volumeId: VolumeId,
        parentId: ParentId,
        nameHashes: List<String>,
        clientUids: List<ClientUid>,
    ): List<PhotoDuplicate>

    fun getPhotoListingsFlow(
        userId: UserId,
        volumeId: VolumeId,
        fromIndex: Int,
        count: Int,
        sortingDirection: Direction = Direction.DESCENDING,
        tag: PhotoTag? = null,
    ): Flow<Result<List<PhotoListing>>>

    suspend fun delete(linkIds: List<LinkId>)

    suspend fun deleteAll(
        userId: UserId,
        volumeId: VolumeId,
        tag: PhotoTag? = null,
    )

    suspend fun insertOrIgnore(volumeId: VolumeId, photoListings: List<PhotoListing>)

    suspend fun getRelatedPhotos(
        volumeId: VolumeId,
        mainFileId: FileId,
        fromIndex: Int,
        count: Int,
    ): List<RelatedPhoto>
}
