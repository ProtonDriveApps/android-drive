/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.photo.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class GetPhotoListingIdsInRangeFlow @Inject constructor(
    private val repository: PhotoRepository,
    private val getPhotoListingIdsInRange: GetPhotoListingIdsInRange,
) {

    operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        tag: PhotoTag?,
        captureTimeFrom: TimestampS,
        captureTimeTo: TimestampS,
    ): Flow<List<FileId>> = repository.getPhotoListingCount(
        userId = userId,
        volumeId = volumeId,
        tag = tag,
    ).map {
        getPhotoListingIdsInRange(
            userId = userId,
            volumeId = volumeId,
            tag = tag,
            captureTimeFrom = captureTimeFrom,
            captureTimeTo = captureTimeTo,
        )
    }.distinctUntilChanged()
}
