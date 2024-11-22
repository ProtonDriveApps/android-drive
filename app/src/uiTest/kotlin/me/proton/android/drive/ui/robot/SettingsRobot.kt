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

import me.proton.android.drive.ui.robot.settings.PhotosBackupRobot
import me.proton.android.drive.ui.screen.SettingsScreenTestTag
import me.proton.core.accountmanager.test.robot.AccountSettingsRobot
import me.proton.core.drive.settings.presentation.SettingsTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object SettingsRobot : NavigationBarRobot {
    private val settingsScreen get() = node.withTag(SettingsScreenTestTag.screen)
    private val settingsList get() = node.withTag(SettingsTestTag.list)
    private val photosBackup get() = node.withText(I18N.string.settings_photos_backup)
    private val account get() = node.withTag(SettingsTestTag.account)
    private val clearLocalCache get() = node.withText(I18N.string.settings_clear_local_cache_entry)
    private val showLog get() = node.withText(I18N.string.settings_show_log)
    private val appVersion get() = node.withTag(SettingsTestTag.appVersion)
    private val messageNotificationLocalCacheClearedSuccessfully get() = node
        .withText(I18N.string.in_app_notification_clear_local_cache_success)

    fun clickToClearLocalCache(): SettingsRobot = apply {
        settingsList.scrollTo(clearLocalCache)
        clearLocalCache.click()
    }
    fun clickToShowLog(): SettingsLogRobot {
        settingsList.scrollTo(showLog)
        return showLog.clickTo(SettingsLogRobot)
    }

    fun clickPhotosBackup() = photosBackup.clickTo(PhotosBackupRobot)

    fun clickAccount() = account.click().let { AccountSettingsRobot }

    fun localCacheClearedSuccessfullyWasShown() = messageNotificationLocalCacheClearedSuccessfully
        .await { assertIsDisplayed() }

    fun assertUserLogIsNotDisplayed() {
        settingsList.scrollTo(appVersion)
        showLog.assertIsNotDisplayed()
    }

    override fun robotDisplayed() {
        settingsScreen.await { assertIsDisplayed() }
    }
}
