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
import me.proton.core.drive.photo.data.api.request.CreatePhotoRequest
import me.proton.core.drive.photo.data.api.request.FindDuplicatesRequest
import me.proton.core.drive.sorting.data.extension.toDtoDesc
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException

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
                )
            }.valueOrThrow.photos

    @Throws(ApiException::class)
    suspend fun findDuplicate(userId: UserId, volumeId: VolumeId, request: FindDuplicatesRequest) =
        apiProvider
            .get<PhotoApi>(userId)
            .invoke {
                findDuplicates(volumeId.id, request)
            }.valueOrThrow
}
