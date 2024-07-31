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

package me.proton.core.drive.eventmanager.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.eventmanager.entity.VolumeConfig
import me.proton.core.drive.volume.domain.entity.VolumeId

interface VolumeConfigRepository {

    /**
     * Stores config for given user id and volume id
     */
    suspend fun add(userId: UserId, volumeId: VolumeId, config: VolumeConfig)

    /**
     * Gets config for given user id and volume id, if such config exists
     */
    suspend fun get(userId: UserId, volumeId: VolumeId): VolumeConfig?

    /**
     * Removes all configs for given user id
     */
    suspend fun removeAll(userId: UserId)
}
