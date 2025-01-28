/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveRatingBooster
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.ratingAndroidDrive
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import javax.inject.Inject

class ShouldShowRatingBooster @Inject constructor(
    private val getFeatureFlag: GetFeatureFlag,
    private val wasRatingBoosterShown: WasRatingBoosterShown,
) {
    suspend operator fun invoke(
        userId: UserId,
    ): Result<Boolean> = runCatching {
        wasRatingBoosterShown().getOrThrow().not()
                && getFeatureFlag(driveRatingBooster(userId)).on
                && getFeatureFlag(ratingAndroidDrive(userId)).on
    }
}
