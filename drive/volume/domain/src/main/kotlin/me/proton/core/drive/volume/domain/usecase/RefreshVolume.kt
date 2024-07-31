/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.volume.domain.usecase

import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.api.ProtonApiCode.NOT_EXISTS
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.repository.VolumeRepository
import me.proton.core.network.domain.hasProtonErrorCode
import javax.inject.Inject

class RefreshVolume @Inject constructor(
    private val repository: VolumeRepository,
    private val getVolume: GetVolume,
) {
    suspend operator fun invoke(userId: UserId, volumeId: VolumeId) = coRunCatching {
        getVolume(
            userId = userId,
            volumeId = volumeId,
            refresh = flowOf(true),
        ).toResult()
            .recoverCatching { error ->
                if (error.hasProtonErrorCode(NOT_EXISTS)) {
                    repository.removeVolume(userId, volumeId)
                } else {
                    throw error
                }
            }
            .getOrThrow()
    }
}
