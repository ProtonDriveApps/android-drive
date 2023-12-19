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
import me.proton.android.drive.ui.test.PhotosBaseTest
import me.proton.android.drive.ui.rules.NetworkSimulator
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class NoConnectivityTest: PhotosBaseTest() {

    @Before
    fun prepare() {
        FilesTabRobot
            .clickPhotosTab()
            .verify {
                robotDisplayed()
                assertEnableBackupDisplayed()
            }
    }

    @Test
    fun backUpWithNoConnectivity() {
        pictureCameraFolder.copyDirFromAssets("images/basic")
        dcimCameraFolder.copyFileFromAssets("boat.mp4")

        NetworkSimulator.disableNetwork()

        PhotosTabRobot
            .enableBackup()
            .verify {
                assertNoConnectivityBannerDisplayed()
                assertNoBackupsDisplayed()
            }

        NetworkSimulator.enableNetwork()

        PhotosTabRobot
            .verify {
                assertPhotoCountEquals(1)
            }

        NetworkSimulator.disableNetwork()

        PhotosTabRobot
            .verify {
                assertNoConnectivityBannerDisplayed()
            }

        NetworkSimulator.enableNetwork()

        PhotosTabRobot
            .verify {
                assertPhotoCountEquals(5)
            }
            .clickFilesTab()
            .verify {
                robotDisplayed()
            }
            .clickPhotosTab()
            .verify {
                assertBackupCompleteDisplayed()
                assertNoConnectivityBannerNotDisplayed()
                assertPhotoCountEquals(5)
            }
    }

    @Test
    fun resumeBackupAfterNetworkTimeout() {
        dcimCameraFolder.copyDirFromAssets("images/basic")

        NetworkSimulator.setNetworkTimeout(true)

        PhotosTabRobot
            .enableBackup()
            .verify {
                assertItCanTakeAwhileDisplayed()
            }

        NetworkSimulator.setNetworkTimeout(false)

        PhotosTabRobot
            .verify {
                assertBackupCompleteDisplayed()
                assertPhotoCountEquals(4)
            }
    }
}
