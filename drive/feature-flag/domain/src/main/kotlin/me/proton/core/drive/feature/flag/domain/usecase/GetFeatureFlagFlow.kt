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
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.repository.FeatureFlagRepository
import javax.inject.Inject

class GetFeatureFlagFlow @Inject constructor(
    private val getFeatureFlag: GetFeatureFlag,
    private val featureFlagRepository: FeatureFlagRepository,
    private val configurationProvider: ConfigurationProvider,
) {

    operator fun invoke(featureFlagId: FeatureFlagId): Flow<FeatureFlag> = flow {
        emit(FeatureFlag(featureFlagId, FeatureFlag.State.NOT_FOUND))
        emit(getFeatureFlag(featureFlagId))
        emitAll(featureFlagRepository
            .getFeatureFlagFlow(featureFlagId)
            .filterNotNull()
            .filterNot { featureFlag ->
                featureFlag.id.id in FeatureFlagId.developments && configurationProvider.disableFeatureFlagInDevelopment
            }
        )
    }.distinctUntilChanged()
}
