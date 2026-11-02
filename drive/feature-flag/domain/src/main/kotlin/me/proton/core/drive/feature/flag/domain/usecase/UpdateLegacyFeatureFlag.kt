/*
 * Copyright (c) 2026 Proton AG.
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

import me.proton.core.drive.base.domain.util.coRunCatchingException
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.repository.FeatureFlagRepository
import javax.inject.Inject

class UpdateLegacyFeatureFlag @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
) {

    suspend operator fun invoke(featureFlagId: FeatureFlagId.Legacy, value: Boolean): Result<Unit> =
        coRunCatchingException {
            // FeatureFlagRepository.update writes nothing and still succeeds when the flag doesn't exist
            featureFlagRepository.getFeatureFlag(featureFlagId)
                ?: throw NoSuchElementException("Feature flag not found")
            featureFlagRepository.update(featureFlagId, value).getOrThrow()
        }
}
