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

package me.proton.android.drive.ui.robot

import me.proton.android.drive.ui.screen.SettingsScreenTestTag
import me.proton.core.drive.settings.presentation.SettingsTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object SettingsRobot : Robot {
    private val settingsScreen get() = node.withTag(SettingsScreenTestTag.screen)
    private val settingsList get() = node.withTag(SettingsTestTag.list)
    private val clearLocalCache get() = node.withText(I18N.string.settings_clear_local_cache_entry)
    private val messageNotificationLocalCacheClearedSuccessfully get() = node
        .withText(I18N.string.in_app_notification_clear_local_cache_success)

    fun clickToClearLocalCache(): SettingsRobot = apply {
        settingsList.scrollTo(clearLocalCache)
        clearLocalCache.click()
    }

    fun localCacheClearedSuccessfullyWasShown() = messageNotificationLocalCacheClearedSuccessfully
        .await { assertIsDisplayed() }

    override fun robotDisplayed() {
        settingsScreen.await { assertIsDisplayed() }
    }
}
