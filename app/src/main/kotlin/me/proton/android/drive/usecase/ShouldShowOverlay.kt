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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.drive.android.settings.domain.UiSettingsRepository
import me.proton.drive.android.settings.domain.entity.UserOverlay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShouldShowOverlay @Inject constructor(
    private val shouldShowOnboarding: ShouldShowOnboarding,
    private val shouldShowWhatsNew: ShouldShowWhatsNew,
    private val shouldShowRatingBooster: ShouldShowRatingBooster,
    private val shouldShowSubscriptionPromo: ShouldShowSubscriptionPromo,
    private val repository: UiSettingsRepository,
) {
    suspend operator fun invoke(userId: UserId): Result<UserOverlay?> = coRunCatching {
        if (repository.hasShownOverlay()) {
            return@coRunCatching null
        }
        if (shouldShowOnboarding(userId).getOrThrow()) {
            return@coRunCatching UserOverlay.Onboarding
        }
        val subscriptionPromo = shouldShowSubscriptionPromo(userId).getOrThrow()
        if (subscriptionPromo != null) {
            return@coRunCatching subscriptionPromo
        }
        val whatsNewKey = shouldShowWhatsNew(userId).getOrThrow()
        if (whatsNewKey != null) {
            UserOverlay.WhatsNew(whatsNewKey)
        } else if (shouldShowRatingBooster(userId).getOrThrow()) {
            UserOverlay.RatingBooster
        } else {
            null
        }
    }
}
