/*
 * Copyright (c) 2025 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.photo.domain.entity.PhotoShareMigrationState
import me.proton.core.drive.photo.domain.repository.PhotoShareMigrationRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import javax.inject.Inject

class TestPhotoShareMigrationRepositoryImpl @Inject constructor() : PhotoShareMigrationRepository {

    override suspend fun startMigration(userId: UserId) {
        error("Test should not start migration")
    }

    override suspend fun fetchMigrationStatus(userId: UserId): Pair<Long, VolumeId?> {
        throw ApiException(
            ApiResult.Error.Http(
                ProtonApiCode.NOT_EXISTS,
                "no migration for E2E test"
            )
        )
    }

    override suspend fun getMigrationState(userId: UserId): PhotoShareMigrationState? = null

    override fun getMigrationStateFlow(userId: UserId): Flow<PhotoShareMigrationState?> =
        flowOf(null)

    override suspend fun setMigrationState(userId: UserId, state: PhotoShareMigrationState) {
        // no nothing
    }

    override fun getPhotosImportantUpdatesLastShownFlow(userId: UserId): Flow<TimestampMs?> {
        return flowOf(TimestampMs())
    }

    override suspend fun setPhotosImportantUpdatesLastShown(
        userId: UserId,
        lastShown: TimestampMs
    ) {
        // do nothing
    }

    override suspend fun saveBucketIds(userId: UserId, bucketsIds: List<Int>) {
        // do nothing
    }

    override suspend fun getBucketIds(userId: UserId): List<Int> {
        return emptyList()
    }

    override suspend fun deleteBucketIds(userId: UserId) {
        // do nothing
    }
}
