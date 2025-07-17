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

package me.proton.android.drive.ui.test.flow.photos.backup

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.PhotosNoPermissionsRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.settings.PhotosBackupRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

@HiltAndroidTest
class NoPermissionsTest : BaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    fun denyPermissionsFromPhotoTab() {
        PhotosTabRobot
            .enableBackup()

        PhotosNoPermissionsRobot
            .denyPermissions(PhotosNoPermissionsRobot)
            .verify {
                robotDisplayed()
            }
            .clickNotNow(PhotosTabRobot)
            .verify {
                assertEnableBackupDisplayed()
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun denyPermissionsFromSettings() {
        PhotosTabRobot
            .verify {
                // Wait for photo share to be created to avoid collision
                assertEnableBackupDisplayed()
            }
            .openSidebarBySwipe()
            .clickSettings()
            .clickPhotosBackup()
            .clickBackupToggle(PhotosNoPermissionsRobot)
            .denyPermissions(PhotosNoPermissionsRobot)
            .verify {
                robotDisplayed()
            }
            .clickNotNow(PhotosBackupRobot)
            .verify {
                robotDisplayed()
            }
    }
}
