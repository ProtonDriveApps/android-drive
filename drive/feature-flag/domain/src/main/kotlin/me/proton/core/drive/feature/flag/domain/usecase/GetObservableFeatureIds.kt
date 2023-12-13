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

package me.proton.core.drive.feature.flag.domain.usecase

import me.proton.core.drive.feature.flag.domain.handler.FeatureFlagHandler
import me.proton.core.featureflag.domain.entity.FeatureId
import javax.inject.Inject

class GetObservableFeatureIds @Inject constructor(
    private val handlers: @JvmSuppressWildcards Map<String, FeatureFlagHandler>,
) {
    operator fun invoke(): Set<FeatureId> = handlers.keys.map { id -> FeatureId(id) }.toSet()
}
