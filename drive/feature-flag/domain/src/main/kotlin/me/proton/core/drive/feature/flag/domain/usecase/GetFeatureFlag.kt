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

package me.proton.core.drive.feature.flag.domain.usecase

import me.proton.core.drive.base.domain.extension.isOlderThen
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.repository.FeatureFlagRepository
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

typealias FeatureFlagCachePolicy = suspend (FeatureFlagId) -> Boolean

class GetFeatureFlag @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val configurationProvider: ConfigurationProvider,
) {

    private val refreshAfterDuration: FeatureFlagCachePolicy = { featureFlagId ->
        featureFlagRepository
            .getLastRefreshTimestamp(featureFlagId.userId)
            .isOlderThen(configurationProvider.featureFlagFreshDuration)
    }

    suspend operator fun invoke(
        featureFlagId: FeatureFlagId,
        refresh: FeatureFlagCachePolicy = refreshAfterDuration,
    ): FeatureFlag {
        if (refresh(featureFlagId)) {
            featureFlagRepository.refresh(featureFlagId.userId).onFailure { error ->
                CoreLogger.w(
                    tag = LogTag.FEATURE_FLAG,
                    e = error,
                    message = "Cannot refresh feature flag: ${featureFlagId.id}",
                )
            }.getOrNull()
        }
        return coRunCatching {
            featureFlagRepository.getFeatureFlag(featureFlagId)
        }.fold(
            onSuccess = { featureFlag -> featureFlag ?: FeatureFlag(featureFlagId, FeatureFlag.State.NOT_FOUND) },
            onFailure = { error ->
                CoreLogger.d(
                    tag = LogTag.FEATURE_FLAG,
                    e = error,
                    message = "Fallback to default value due to exception",
                )
                FeatureFlag(featureFlagId, FeatureFlag.State.DISABLED)
            },
        )
    }
}
