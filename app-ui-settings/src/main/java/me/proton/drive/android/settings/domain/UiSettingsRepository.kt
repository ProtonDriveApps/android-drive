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
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.drive.android.settings.domain.entity.LayoutType
import me.proton.drive.android.settings.domain.entity.ThemeStyle
import me.proton.drive.android.settings.domain.entity.UiSettings

interface UiSettingsRepository {
    /**
     * Retrieve the [UiSettings] for a given [userId]
     */
    suspend fun getUiSettings(userId: UserId) = getUiSettingsFlow(userId).first()

    /**
     * A [Flow] of [UiSettings] for a given [userId] which emits when the settings for that [userId] changes
     */
    fun getUiSettingsFlow(userId: UserId): Flow<UiSettings>

    /**
     * Update the [UiSettings] for a given [userId]
     */
    suspend fun updateUiSettings(userId: UserId, settings: UiSettings)

    /**
     * Update the [LayoutType] for a given [userId]
     */
    suspend fun updateLayoutType(userId: UserId, layoutType: LayoutType)

    /**
     * Update the [ThemeStyle] for a given [userId]
     */
    suspend fun updateThemeStyle(userId: UserId, themeStyle: ThemeStyle)
}
