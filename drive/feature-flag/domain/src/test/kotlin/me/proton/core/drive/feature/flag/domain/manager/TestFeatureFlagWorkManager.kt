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

package me.proton.core.drive.feature.flag.domain.manager

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.feature.flag.domain.usecase.RefreshFeatureFlags
import javax.inject.Inject

class TestFeatureFlagWorkManager @Inject constructor(
    private val refreshFeatureFlags: RefreshFeatureFlags,
) : FeatureFlagWorkManager {
    override suspend fun start(userId: UserId) {
        enqueue(userId)
    }

    override suspend fun stop(userId: UserId) {
        TODO("Not yet implemented")
    }

    override suspend fun enqueue(userId: UserId) {
        refreshFeatureFlags(userId).getOrThrow()
    }

    override suspend fun cancel(userId: UserId) {
        TODO("Not yet implemented")
    }
}
