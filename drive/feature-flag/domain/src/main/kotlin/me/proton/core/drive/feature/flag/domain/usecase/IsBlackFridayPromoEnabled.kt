/*
 * Copyright (c) 2025 Proton AG.
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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidBlackFriday2025
import me.proton.core.drive.feature.flag.domain.extension.on
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.inject.Inject

class IsBlackFridayPromoEnabled @Inject constructor(
    private val getFeatureFlag: GetFeatureFlag,
) {
    private val limitUtc = ZonedDateTime
        .of(2025, 12, 3, 23, 59, 59, 0, ZoneOffset.UTC)
        .toInstant()
    private val nowUtc: Instant = Instant.now()

    suspend operator fun invoke(userId: UserId) =
        nowUtc.isBefore(limitUtc) && isBlackFridayPromoEnabled(userId)

    private suspend fun isBlackFridayPromoEnabled(userId: UserId) =
        getFeatureFlag(driveAndroidBlackFriday2025(userId)).on
}
