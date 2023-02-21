/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.volume.data.api

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.volume.data.api.request.CreateVolumeRequest
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException

class VolumeApiDataSource(private val apiProvider: ApiProvider) {
    @Throws(ApiException::class)
    suspend fun getVolumes(userId: UserId): List<VolumeDto> =
        apiProvider.get<VolumeApi>(userId).invoke { getVolumes() }.valueOrThrow.volumeDtos

    @Throws(ApiException::class)
    suspend fun getVolume(userId: UserId, volumeId: String): VolumeDto =
        apiProvider.get<VolumeApi>(userId).invoke { getVolume(volumeId) }.valueOrThrow.volumeDto

    @Throws(ApiException::class)
    suspend fun createVolume(userId: UserId, request: CreateVolumeRequest) =
        apiProvider.get<VolumeApi>(userId).invoke { createVolume(request) }.valueOrThrow.volumeDto
}
