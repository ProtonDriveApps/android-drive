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
package me.proton.core.drive.volume.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.entity.VolumeInfo


interface VolumeRepository {

    /**
     * Get reactive list of all drive volumes for given user
     */
    fun getVolumesFlow(userId: UserId): Flow<DataResult<List<Volume>>>

    /**
     * Check if we have cached any volume for given user
     */
    suspend fun hasVolumes(userId: UserId): Boolean

    /**
     * Fetches volumes from the server and stores it into cache
     */
    suspend fun fetchVolumes(userId: UserId): List<Volume>

    /**
     * Get reactive volume for given user and volume id
     */
    fun getVolumeFlow(userId: UserId, volumeId: VolumeId): Flow<DataResult<Volume>>

    /**
     * Check if we have cached volume for given user and volume id
     */
    suspend fun hasVolume(userId: UserId, volumeId: VolumeId): Boolean

    /**
     * Fetches volume from the server and stores it into cache
     */
    suspend fun fetchVolume(userId: UserId, volumeId: VolumeId)

    /**
     * Creates new volume for user and return volume id
     */
    fun createVolume(userId: UserId, volumeInfo: VolumeInfo): Flow<DataResult<Volume>>

    /**
     * Removes volume for a given user from the cache
     */
    suspend fun removeVolume(userId: UserId, volumeId: VolumeId)
}
