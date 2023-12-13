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

package me.proton.core.drive.photo.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.usecase.GetOldestActiveVolume
import javax.inject.Inject

class GetPhotoCount @Inject constructor(
    private val getOldestActiveVolume: GetOldestActiveVolume,
    private val repository: PhotoRepository,
) {
    operator fun invoke(userId: UserId): Flow<Int> = getOldestActiveVolume(userId)
        .filterSuccessOrError()
        .mapSuccessValueOrNull()
        .filterNotNull()
        .flatMapLatest { volume -> invoke(userId, volume.id) }

    operator fun invoke(userId: UserId, volumeId: VolumeId): Flow<Int> =
        repository.getPhotoListingCount(userId, volumeId)

}
