/*
 * Copyright (c) 2026 Proton AG.
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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.datastore.GetUserDataStore.Keys.q3CampaignPromo2026LastShown
import me.proton.core.drive.base.data.extension.get
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.util.coRunCatching
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class ShouldShowQ3CampaignPromo @Inject constructor(
    private val isQ3CampaignPromoEligible: IsQ3CampaignPromoEligible,
    private val areNotificationsEnabled: AreNotificationsEnabled,
    private val getUserDataStore: GetUserDataStore,
) {
    // Both periods are defined in CEST (Europe/Zurich), confirmed with the campaign spec.
    private val timeframes = listOf(
        Timeframe(
            begin = LocalDateTime
                .of(2026, 8, 31, 12, 0, 0)
                .atZone(zone)
                .toInstant(),
            end = LocalDateTime
                .of(2026, 9, 6, 12, 0, 0)
                .atZone(zone)
                .toInstant(),
        ),
        Timeframe(
            begin = LocalDateTime
                .of(2026, 9, 7, 12, 0, 0)
                .atZone(zone)
                .toInstant(),
            end = LocalDateTime
                .of(2026, 9, 11, 12, 0, 0)
                .atZone(zone)
                .toInstant(),
        ),
    )

    suspend operator fun invoke(
        userId: UserId,
        now: Instant = Instant.now()
    ): Result<Boolean> = coRunCatching {
        isQ3CampaignPromoEligible(userId) &&
            areNotificationsEnabled() &&
            isWithinTimeframe(now) &&
            wasNotAlreadyShown(userId, now)
    }

    private suspend fun wasNotAlreadyShown(userId: UserId, now: Instant): Boolean =
        timeframes.firstOrNull { timeframe -> now.isWithinTimeframe(timeframe) }?.let { currentTimeframe ->
            getLastShown(userId)
                ?.toInstant()
                ?.isNotWithinTimeframe(currentTimeframe)
                ?: true
        } ?: true

    private fun isWithinTimeframe(now: Instant): Boolean = timeframes.any { timeframe ->
        now.isWithinTimeframe(timeframe)
    }

    private fun Instant.isWithinTimeframe(timeframe: Timeframe): Boolean =
        isAfter(timeframe.begin) && isBefore(timeframe.end)

    private fun Instant.isNotWithinTimeframe(timeframe: Timeframe): Boolean = !this.isWithinTimeframe(timeframe)

    private fun TimestampMs.toInstant(): Instant = Instant.ofEpochMilli(value)

    private suspend fun getLastShown(userId: UserId): TimestampMs? =
        getUserDataStore(userId).get(q3CampaignPromo2026LastShown)?.let { TimestampMs(it) }

    data class Timeframe(
        val begin: Instant,
        val end: Instant,
    )

    companion object {
        private val zone = ZoneId.of("Europe/Zurich")
    }
}
