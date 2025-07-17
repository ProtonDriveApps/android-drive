/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.test.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import me.proton.core.drive.base.domain.log.LogTag.FEATURE_FLAG
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.repository.FeatureFlagRepository
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class TestFeatureFlagRepository @Inject constructor(
    private val repository: FeatureFlagRepository,
) : FeatureFlagRepository by repository {

    override suspend fun getFeatureFlag(featureFlagId: FeatureFlagId): FeatureFlag? =
        flags[featureFlagId.id]?.let { state: FeatureFlag.State ->
            FeatureFlag(featureFlagId, state).also { it.log("local") }
        } ?: repository.getFeatureFlag(featureFlagId).also { it?.log("remote") }

    override suspend fun getFeatureFlagFlow(featureFlagId: FeatureFlagId): Flow<FeatureFlag?> =
        flags[featureFlagId.id]?.let { state ->
            flowOf(FeatureFlag(featureFlagId, state)).onEach { it.log("local") }
        } ?: repository.getFeatureFlagFlow(featureFlagId).onEach { it?.log("remote") }


    private fun FeatureFlag.log(source: String) {
        CoreLogger.d(FEATURE_FLAG, "Use $source feature flag $id to $state")
    }

    companion object {
        val flags = mutableMapOf<String, FeatureFlag.State>()
    }
}
