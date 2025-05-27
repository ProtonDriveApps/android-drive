/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.ui.robot.settings

import me.proton.android.drive.ui.robot.NavigationBarRobot
import me.proton.android.drive.ui.robot.Robot
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object PhotosBackupRobot : Robot, NavigationBarRobot {

    private val photosBackupToggle =
        node.isCheckable().hasDescendant(node.withText(I18N.string.photos_backup_title))
    private val uploadFrom get() = node.withText(I18N.string.settings_photos_backup_folders_title)

    private val confirmButton =
        node.withText(I18N.string.settings_photos_backup_folders_confirm_stop_sync_confirm_action)

    private val photosBackupTurnOn get() = node.withText(I18N.string.photos_backup_in_app_notification_turned_on)
    private val photosBackupDisabled get() = node.withText(I18N.string.error_creating_photo_share_not_allowed)

    fun <T : Robot> clickBackupToggle(goesTo: T) = photosBackupToggle.clickTo(goesTo)

    fun assertFoldersList() = uploadFrom.await { assertIsDisplayed() }
    fun assertFolderNotFound(name: String) = node.withText(name)
        .hasSibling(node.withText(I18N.string.settings_photos_backup_folders_not_found))
        .await { assertIsDisplayed() }

    fun assertFolderEnable(name: String) = node.isCheckable().hasDescendant(node.withText(name)).await {
        assertIsAsserted()
    }
    fun assertFolderDisable(name: String) = node.isCheckable().hasDescendant(node.withText(name)).await {
        assertIsOff()
    }

    fun clickFolder(name: String) = node.withText(name).clickTo(PhotosBackupRobot)
    fun clickConfirm() = confirmButton.clickTo(PhotosBackupRobot)

    fun assertPhotosShareCreationDisabled() {
        photosBackupDisabled.await { assertIsDisplayed() }
    }

    fun assertPhotosBackupTurnOn() {
        photosBackupTurnOn.await { assertIsDisplayed() }
    }

    override fun robotDisplayed() {
        photosBackupToggle.await { assertIsDisplayed() }
    }
}
