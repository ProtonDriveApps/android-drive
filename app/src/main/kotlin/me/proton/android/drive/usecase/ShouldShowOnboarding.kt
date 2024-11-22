/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.usecase

import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.HasBusinessPlan
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidNewOnboarding
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShouldShowOnboarding @Inject constructor(
    private val isFirstAppUsage: IsFirstAppUsage,
    private val wasOnboardingShown: WasOnboardingShown,
    private val getFeatureFlagFlow: GetFeatureFlagFlow,
    private val hasBusinessPlan: HasBusinessPlan,
) {

    suspend operator fun invoke(userId: UserId): Result<Boolean> = coRunCatching {
        when {
            isFirstAppUsage() -> false
            wasOnboardingShown().getOrThrow() -> false
            hasBusinessPlan(userId).getOrThrow() -> false
            else -> getFeatureFlagFlow(
                featureFlagId = driveAndroidNewOnboarding(userId),
                emitNotFoundInitially = false,
            ).first().on
        }
    }
}
