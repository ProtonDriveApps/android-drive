/*
 * Copyright (c) 2023-2024 Proton AG.
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
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId

interface FeatureFlagRepository {

    suspend fun getFeatureFlag(featureFlagId: FeatureFlagId): FeatureFlag?

    suspend fun getFeatureFlagFlow(featureFlagId: FeatureFlagId): Flow<FeatureFlag?>

    suspend fun refresh(userId: UserId, refreshId: RefreshId = RefreshId.DEFAULT): Result<Unit>

    suspend fun getLastRefreshTimestamp(userId: UserId, refreshId: RefreshId = RefreshId.DEFAULT): TimestampMs?

    enum class RefreshId(val id: String) {
        DEFAULT("default_id"),
        API_ERROR_FEATURE_DISABLED("api_error_feature_disabled_id")
    }
}
