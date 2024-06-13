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

package me.proton.core.drive.feature.flag.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.repository.FeatureFlagRepository
import javax.inject.Inject

class GetFeatureFlagFlow @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val configurationProvider: ConfigurationProvider,
) : BaseGetFeatureFlag(featureFlagRepository, configurationProvider) {

    operator fun invoke(
        featureFlagId: FeatureFlagId,
        refresh: FeatureFlagCachePolicy = refreshAfterDuration,
    ): Flow<FeatureFlag> = flow {
        emit(FeatureFlag(featureFlagId, FeatureFlag.State.NOT_FOUND))
        if (refresh(featureFlagId)) {
            refreshFeatureFlag(featureFlagId)
        }
        emitAll(
            getFeatureFlagFlow(featureFlagId).map { featureFlag ->
                featureFlag ?: FeatureFlag(featureFlagId, FeatureFlag.State.NOT_FOUND)
            }
        )
    }.distinctUntilChanged()

    private suspend fun getFeatureFlagFlow(featureFlagId: FeatureFlagId?): Flow<FeatureFlag?> =
        featureFlagId?.takeUnless { id ->
            id.id in FeatureFlagId.developments && configurationProvider.disableFeatureFlagInDevelopment
        }?.let { id ->
            featureFlagRepository.getFeatureFlagFlow(id)
        } ?: flowOf<FeatureFlag?>(null)
}
