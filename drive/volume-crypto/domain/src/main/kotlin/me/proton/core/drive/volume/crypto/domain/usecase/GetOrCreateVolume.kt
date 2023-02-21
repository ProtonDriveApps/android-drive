/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.volume.crypto.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.asSuccessOrNullAsError
import me.proton.core.drive.base.domain.extension.transformSuccess
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.entity.isActive
import me.proton.core.drive.volume.domain.repository.VolumeRepository
import me.proton.core.drive.volume.domain.usecase.GetVolumes
import javax.inject.Inject

class GetOrCreateVolume @Inject constructor(
    private val getVolumes: GetVolumes,
    private val createVolume: CreateVolume,
    private val volumeRepository: VolumeRepository,
) {
    @ExperimentalCoroutinesApi
    operator fun invoke(userId: UserId): Flow<DataResult<Volume>> =
        getVolumes(userId).transformSuccess { (_, volumes) ->
            val activeVolumes = volumes.filter { volume ->
                volume.isActive.also { isActive ->
                    if (!isActive) volumeRepository.removeVolume(userId, volume.id)
                }
            }
            if (activeVolumes.isEmpty()) {
                emitAll(createVolume(userId))
            } else {
                emit(activeVolumes
                    .minByOrNull { volume -> volume.creationTime.value }
                    .asSuccessOrNullAsError()
                )
            }
        }
}
