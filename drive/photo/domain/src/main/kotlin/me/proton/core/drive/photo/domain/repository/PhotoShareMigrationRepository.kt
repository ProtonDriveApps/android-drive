/*
 * Copyright (c) 2025 Proton AG.
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
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.photo.domain.entity.PhotoShareMigrationState
import me.proton.core.drive.volume.domain.entity.VolumeId

interface PhotoShareMigrationRepository {

    suspend fun startMigration(userId: UserId)

    suspend fun fetchMigrationStatus(userId: UserId): Pair<Long, VolumeId?>

    suspend fun getMigrationState(userId: UserId): PhotoShareMigrationState?

    fun getMigrationStateFlow(userId: UserId): Flow<PhotoShareMigrationState?>

    suspend fun setMigrationState(userId: UserId, state: PhotoShareMigrationState)

    fun getPhotosImportantUpdatesLastShownFlow(userId: UserId): Flow<TimestampMs?>

    suspend fun setPhotosImportantUpdatesLastShown(userId: UserId, lastShown: TimestampMs)
}
