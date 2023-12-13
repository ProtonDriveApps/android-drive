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

package me.proton.core.drive.feature.flag.data.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.data.db.DriveFeatureFlagDatabase
import me.proton.core.drive.feature.flag.data.db.entity.DriveFeatureFlagRefreshEntity
import me.proton.core.drive.feature.flag.data.extension.toFeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.repository.FeatureFlagRepository
import me.proton.core.featureflag.domain.entity.FeatureId
import javax.inject.Inject
import me.proton.core.featureflag.domain.repository.FeatureFlagRepository as CoreFeatureFlagRepository

class FeatureFlagRepositoryImpl @Inject constructor(
    private val coreFeatureFlagRepository: CoreFeatureFlagRepository,
    private val db: DriveFeatureFlagDatabase
) : FeatureFlagRepository {

    override suspend fun getFeatureFlag(featureFlagId: FeatureFlagId): FeatureFlag? =
        coreFeatureFlagRepository.get(
            featureFlagId.userId,
            FeatureId(featureFlagId.id),
        )?.toFeatureFlag(featureFlagId.userId)

    override suspend fun refresh(userId: UserId, refreshId: FeatureFlagRepository.RefreshId) = coRunCatching {
        coreFeatureFlagRepository.getAll(userId)
        db.driveFeatureFlagRefreshDao.insertOrUpdate(
            DriveFeatureFlagRefreshEntity(
                userId = userId,
                id = refreshId.id,
                lastFetchTimestamp = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun getLastRefreshTimestamp(
        userId: UserId,
        refreshId: FeatureFlagRepository.RefreshId,
    ): TimestampMs? =
        db.driveFeatureFlagRefreshDao.get(userId, refreshId.id)?.let { value ->
            TimestampMs(value)
        }
}
