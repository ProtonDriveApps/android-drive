/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.settings.presentation.state

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import me.proton.core.drive.settings.presentation.component.DebugSettingsStateAndEvent
import kotlin.time.Duration

data class SettingsViewState(
    @DrawableRes val navigationIcon: Int,
    @StringRes val appNameResId: Int,
    val appVersion: String,
    val legalLinks: List<LegalLink>,
    val availableStyles: List<Int>,
    @StringRes val currentStyle: Int,
    val debugSettingsStateAndEvent: DebugSettingsStateAndEvent? = null,
    @StringRes val appAccessSubtitleResId: Int,
    val isAutoLockDurationsVisible: Boolean,
    val autoLockDuration: Duration,
    val isPhotosSettingsVisible : Boolean,
    @StringRes val photosBackupSubtitleResId: Int,
)

sealed class LegalLink(
    @StringRes open val text: Int,
) {

    data class External(
        @StringRes override val text: Int,
        @StringRes val url: Int,
    ) : LegalLink(text)
}
