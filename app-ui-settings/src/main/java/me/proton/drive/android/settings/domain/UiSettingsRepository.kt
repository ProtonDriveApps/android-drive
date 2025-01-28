/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.drive.android.settings.domain

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.drive.android.settings.domain.entity.HomeTab
import me.proton.drive.android.settings.domain.entity.LayoutType
import me.proton.drive.android.settings.domain.entity.ThemeStyle
import me.proton.drive.android.settings.domain.entity.UiSettings
import me.proton.drive.android.settings.domain.entity.WhatsNewKey

interface UiSettingsRepository {

    /**
     * A [Flow] of [UiSettings] for a given [userId] which emits when the settings for that [userId] changes
     */
    fun getUiSettingsFlow(userId: UserId): Flow<UiSettings>

    /**
     * Update the [LayoutType] for a given [userId]
     */
    suspend fun updateLayoutType(userId: UserId, layoutType: LayoutType)

    /**
     * Update the [ThemeStyle] for a given [userId]
     */
    suspend fun updateThemeStyle(userId: UserId, themeStyle: ThemeStyle)

    /**
     * Update the [HomeTab] for a given [userId]
     */
    suspend fun updateHomeTab(userId: UserId, homeTab: HomeTab)

    /**
     * Retrieves setting if onboarding screen has been shown to the user
     */
    suspend fun hasShownOnboarding(): Boolean

    /**
     * Update timestamp when onboarding was shown
     */
    suspend fun updateOnboardingShown(timestamp: TimestampS)

    /**
     * Retrieves setting if whats new screen has been shown to the user
     */
    suspend fun hasShownWhatsNew(key: WhatsNewKey): Boolean

    /**
     * Update timestamp when whats new was shown
     */
    suspend fun updateWhatsNewShown(key: WhatsNewKey, timestamp: TimestampS)

    /**
     * Retrieves setting if rating booster screen has been shown to the user
     */
    suspend fun hasShownRatingBooster(): Boolean

    /**
     * Update timestamp when rating booster was shown
     */
    suspend fun updateRatingBoosterShown(timestamp: TimestampS)

    /**
     * Retrieves setting if any overlay screen has been shown to the user
     */
    suspend fun hasShownOverlay(): Boolean


}
