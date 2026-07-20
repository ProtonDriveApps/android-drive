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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.extension.hasSubscription
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class IsSummerSalePromoEnabled @Inject constructor(
    private val getFeatureFlag: GetFeatureFlag,
    private val userManager: UserManager,
) {
    // Time limit: July 7th, 2026 at 12:00 CEST
    private val limitUtc =
        LocalDateTime
            .of(2026, 7, 7, 12, 0, 0)
            .atZone(zone)
            .toInstant()

    private val nowUtc: Instant get() = Instant.now()

    suspend operator fun invoke(userId: UserId): Boolean =
        nowUtc.isBefore(limitUtc) && isSummerSalePromoEnabled(userId) && isUserWithoutProtonSubscription(userId)

    private suspend fun isSummerSalePromoEnabled(userId: UserId): Boolean =
        getFeatureFlag(FeatureFlagId.driveAndroidSummerSale2026(userId)).on

    private suspend fun isUserWithoutProtonSubscription(userId: UserId): Boolean =
        userManager.getUser(userId).hasSubscription().not()

    companion object {
        private val zone = ZoneId.of("Europe/Zurich")
    }
}
