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

package me.proton.android.drive.ui.test.flow.photos

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.annotation.Quota
import me.proton.android.drive.ui.test.PhotosBaseTest
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class UsedSpaceTest : PhotosBaseTest() {

    @Before
    fun prepare() {
        dcimCameraFolder.copyFileFromAssets("boat.jpg")

        FilesTabRobot
            .clickPhotosTab()
            .enableBackup()
    }

    @Test
    @Quota(percentageFull = 100)
    fun storageFull() {
        PhotosTabRobot
            .verify {
                assertBackupFailed()
                assertStorageFull()
            }
    }

    @Test
    @Quota(percentageFull = 50)
    fun storageHalfFull() {
        PhotosTabRobot
            .verify {
                assertBackupCompleteDisplayed()
                assertStorageFull(50)
            }
            .clickGetMoreStorage()
            .verifySubscriptionIsShown()
    }

    @Test
    @Quota(percentageFull = 80)
    fun storage80PercentFull() {
        PhotosTabRobot
            .verify {
                assertBackupCompleteDisplayed()
                assertStorageFull(80)
            }
            .dismissQuotaBanner()
            .verify {
                assertPhotoCountEquals(1)
            }
    }
}
