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

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.feature.flag.domain.entity.BlackFriday2026Promo
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidBlackFriday2026
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidBlackFriday2026Upgrade
import me.proton.core.drive.feature.flag.domain.extension.on
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class IsBlackFriday2026PromoEnabled @Inject constructor(
    private val getFeatureFlagFlow: GetFeatureFlagFlow,
) {

    operator fun invoke(
        userId: UserId,
        now: Instant = Instant.now(),
    ): Flow<Set<BlackFriday2026Promo>> = combine(
        isOn(driveAndroidBlackFriday2026(userId)),
        isOn(driveAndroidBlackFriday2026Upgrade(userId)),
        isCampaignRunning(now),
    ) { isFreeOn, isUpgradeOn, isCampaignRunning ->
        buildSet {
            if (isCampaignRunning && isFreeOn) add(BlackFriday2026Promo.FREE)
            if (isCampaignRunning && isUpgradeOn) add(BlackFriday2026Promo.UPGRADE)
        }
    }.distinctUntilChanged()

    // Cache only: FeatureFlagRefreshWorker keeps the flags current, and eligibility must not wait for the network
    private fun isOn(featureFlagId: FeatureFlagId): Flow<Boolean> =
        getFeatureFlagFlow(featureFlagId, refresh = { false }, emitNotFoundInitially = false)
            .map { featureFlag -> featureFlag.on }

    private fun isCampaignRunning(now: Instant): Flow<Boolean> = flow {
        val remaining = Duration.between(now, END)
        if (remaining.isNegative || remaining.isZero) {
            emit(false)
        } else {
            emit(true)
            delay(remaining.toMillis())
            emit(false)
        }
    }

    companion object {
        val END: Instant = LocalDateTime
            .of(2026, 12, 1, 12, 0, 0)
            .atZone(ZoneId.of("Europe/Zurich"))
            .toInstant()
    }
}
