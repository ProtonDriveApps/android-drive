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

package me.proton.android.drive.ui.test.flow.offline

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.data.ImageName
import me.proton.android.drive.ui.robot.FileFolderOptionsRobot
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.NetworkSimulator
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.drive.files.presentation.extension.SemanticsDownloadState
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class MakeAvailableOfflineFlowTest : BaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 1)
    fun makeFileAvailableOffline() {
        val folder = "folder1"
        val file = "presentation.pdf"
        PhotosTabRobot
            .clickFilesTab()
            .clickOnFolder(folder)
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickMakeAvailableOffline()
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 1)
    fun makeFolderWithChildFolderAvailableOffline() {
        val folder = "folder2"
        val subfolder = "folder5"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickMakeAvailableOffline()
            .verify {
                itemIsDisplayed(folder, downloadState = SemanticsDownloadState.Downloaded)
            }
            .clickOnFolder(folder)
            .verify {
                itemIsDisplayed(subfolder, downloadState = SemanticsDownloadState.Downloaded)
            }
            .clickBack(FilesTabRobot)
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsDisplayed(folder, downloadState = SemanticsDownloadState.Downloaded)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 4)
    fun makeEmptyFolderAvailableOffline() {
        val folder = "folder1"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickMakeAvailableOffline()
            .verify {
                itemIsDisplayed(folder, downloadState = SemanticsDownloadState.Downloaded)
            }
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsDisplayed(folder, downloadState = SemanticsDownloadState.Downloaded)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    fun makeMultiplePhotosAvailableOffline() {
        val firstImage = ImageName.Yesterday
        val secondImage = ImageName.Now

        PhotosTabRobot.waitUntilLoaded()
        PhotosTabRobot
            .verify {
                assertPhotoDisplayed(firstImage)
                assertPhotoDisplayed(secondImage)
            }
            .longClickOnPhoto(firstImage.fileName)
            .clickOptions()
            .clickMakeAvailableOffline(goesTo = PhotosTabRobot)
            .verify {
                robotDisplayed()
            }
            .deselectPhoto(firstImage)
            .longClickOnPhoto(secondImage.fileName)
            .clickOptions()
            .clickMakeAvailableOffline(goesTo = PhotosTabRobot)
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsDisplayed(firstImage.fileName)
                itemIsDisplayed(secondImage.fileName)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 4)
    fun makeFolderWithFileInsideAvailableOffline() {
        val folder = "sharedFolder"
        val file = "sharedChild.html"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickMakeAvailableOffline()
            .verify {
                itemIsDisplayed(folder, downloadState = SemanticsDownloadState.Downloaded)
            }
            .clickOnFolder(folder)
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
            .clickBack(FilesTabRobot)
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsDisplayed(folder, downloadState = SemanticsDownloadState.Downloaded)
            }
            .clickOnFolder(folder)
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 4)
    fun makeFolderAndChildFileAvailableOfflineSeparately() {
        val folder = "sharedFolder"
        val file = "sharedChild.html"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickMakeAvailableOffline()
            .clickOnFolder(folder)
            .clickMoreOnItem(file)
            .clickMakeAvailableOffline()
            .clickBack(FilesTabRobot)
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsDisplayed(folder, downloadState = SemanticsDownloadState.Downloaded)
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun loseConnectionWhenDownloadingIsStarted() {

        val file = "image.jpg"

        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)

        NetworkSimulator.disableNetworkFor(5.seconds) {
            FileFolderOptionsRobot
                .clickMakeAvailableOffline()
        }

        FilesTabRobot.verify {
            itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
        }
    }
}
