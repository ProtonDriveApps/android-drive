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

package me.proton.android.drive.ui.test.flow.photos

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.NetworkSimulator
import me.proton.android.drive.ui.test.PhotosBaseTest
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class NoConnectivityTest: PhotosBaseTest() {

    @Before
    fun prepare() {
        pictureCameraFolder.copyDirFromAssets("images/basic")
        PhotosTabRobot
            .verify {
                robotDisplayed()
                assertEnableBackupDisplayed()
            }
    }

    @Test
    fun backUpWithNoConnectivity() {

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
                assertBackupCompleteDisplayed()
                assertNoConnectivityBannerNotDisplayed()
                assertPhotoCountEquals(4)
            }
    }

    @Test
    fun resumeBackupAfterNetworkTimeout() {

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
