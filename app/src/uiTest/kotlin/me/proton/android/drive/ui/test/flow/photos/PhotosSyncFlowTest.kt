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
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.PhotosBaseTest
import org.junit.Test

@HiltAndroidTest
class PhotosSyncFlowTest : PhotosBaseTest() {
    @Test
    @Scenario(2)
    fun syncMultipleFolders() {
        pictureCameraFolder.copyDirFromAssets("images/basic", flatten = true)
        dcimCameraFolder.copyFileFromAssets("boat.jpg")

        FilesTabRobot
            .clickPhotosTab()
            .enableBackup()
            .verify {
                assertBackupCompleteDisplayed()
                assertPhotoDisplayed("boat.jpg")
                assertPhotoCountEquals(5)
            }
    }

    @Test
    @Scenario(2)
    fun syncNewPhotos() {
        dcimCameraFolder.copyDirFromAssets("images/basic", flatten = true)

        FilesTabRobot
            .clickPhotosTab()
            .enableBackup()
            .verify {
                assertBackupCompleteDisplayed()
                assertPhotoCountEquals(4)
            }

        dcimCameraFolder.copyFileFromAssets("boat.jpg")

        PhotosTabRobot
            .verify {
                assertPhotoDisplayed("boat.jpg")
                assertPhotoCountEquals(5)
            }
    }

    @Test
    @Scenario(2)
    fun addPhotoWhileUploading() {
        dcimCameraFolder.copyDirFromAssets("images/basic", flatten = true)
        dcimCameraFolder.copyFileFromAssets("boat.mp4")

        FilesTabRobot
            .clickPhotosTab()
            .enableBackup()

        dcimCameraFolder.copyFileFromAssets("boat.jpg")

        PhotosTabRobot
            .verify {
                assertBackupCompleteDisplayed()
                assertPhotoDisplayed("boat.jpg")
                assertPhotoDisplayed("boat.mp4")
                assertPhotoCountEquals(6)
            }
    }

    @Test
    @Scenario(2)
    fun backupVideoAndMakeAvailableOffline() {
        val videoFile = "boat.mp4"
        dcimCameraFolder.copyFileFromAssets(videoFile)

        FilesTabRobot
            .clickPhotosTab()
            .enableBackup()
            .verify {
                assertPhotoDisplayed(videoFile)
            }
            .longClickOnPhoto(videoFile)
            .clickOptions()
            .clickMakeAvailableOffline(goesTo = PhotosTabRobot)
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsDisplayed(videoFile)
            }
    }

    @Test
    @Scenario(2)
    fun turnOnBackupFromSettings() {
        dcimCameraFolder.copyFileFromAssets("boat.jpg")

        FilesTabRobot
            .openSidebarBySwipe()
            .clickSettings()
            .clickPhotosBackup()
            .clickBackupToggle()
            .verify {
                robotDisplayed()
            }
            .clickBack(FilesTabRobot)
            .clickPhotosTab()
            .verify {
                assertPhotoDisplayed("boat.jpg")
            }
    }
}
