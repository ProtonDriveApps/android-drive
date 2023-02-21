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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.GetSignatureAddress
import me.proton.core.drive.crypto.domain.usecase.volume.CreateVolumeInfo
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.repository.VolumeRepository
import javax.inject.Inject

class CreateVolume @Inject constructor(
    private val volumeRepository: VolumeRepository,
    private val createVolumeInfo: CreateVolumeInfo,
    private val getSignatureAddress: GetSignatureAddress,
) {
    operator fun invoke(userId: UserId): Flow<DataResult<Volume>> = flow {
        try {
            emitAll(volumeRepository.createVolume(userId, createVolumeInfo(userId, getSignatureAddress(userId)).getOrThrow()))
        } catch (e: Exception) {
            emit(DataResult.Error.Local(null, e))
        }
    }
}
