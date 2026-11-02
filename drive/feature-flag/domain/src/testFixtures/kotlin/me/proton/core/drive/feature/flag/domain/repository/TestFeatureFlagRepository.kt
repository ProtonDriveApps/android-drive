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

package me.proton.core.drive.feature.flag.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId

class TestFeatureFlagRepository(
    val featureFlags: MutableMap<FeatureFlagId, FeatureFlag> = mutableMapOf()
) : FeatureFlagRepository {

    var refreshResult: Result<Unit> = Result.success(Unit)
    var timestamp: TimestampMs? = null
    var updateResult: Result<Unit> = Result.success(Unit)

    override suspend fun getFeatureFlag(featureFlagId: FeatureFlagId): FeatureFlag? =
        featureFlags[featureFlagId]

    override suspend fun getFeatureFlagFlow(featureFlagId: FeatureFlagId): Flow<FeatureFlag?> =
        flow {
            emit(featureFlags[featureFlagId])
        }

    override suspend fun refresh(
        featureFlagId: FeatureFlagId,
        refreshId: FeatureFlagRepository.RefreshId
    ): Result<Unit> =
        refreshResult

    override suspend fun refresh(
        userId: UserId,
        refreshId: FeatureFlagRepository.RefreshId
    ): Result<Unit> =
        refreshResult

    override suspend fun getLastRefreshTimestamp(
        userId: UserId,
        refreshId: FeatureFlagRepository.RefreshId
    ): TimestampMs? = timestamp

    override suspend fun update(
        featureFlagId: FeatureFlagId.Legacy,
        value: Boolean
    ): Result<Unit> {
        val featureFlag = featureFlags[featureFlagId] ?: return Result.failure(NoSuchElementException())
        val state = if (value) FeatureFlag.State.ENABLED else FeatureFlag.State.DISABLED
        featureFlags[featureFlagId] = featureFlag.copy(state = state)
        return Result.success(Unit)
    }
}