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

package me.proton.android.drive.ui.test.flow.offline

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.data.ImageName
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.drive.files.presentation.extension.SemanticsDownloadState
import org.junit.Test

@HiltAndroidTest
class MakeAvailableOfflineFlowTest : AuthenticatedBaseTest() {
    @Test
    @Scenario(1)
    fun makeFileAvailableOffline() {
        val folder = "folder1"
        val file = "presentation.pdf"
        FilesTabRobot
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
    @Scenario(1)
    fun makeFolderWithChildFolderAvailableOffline() {
        val folder = "folder2"
        val subfolder = "folder5"
        FilesTabRobot
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
    @Scenario(4)
    fun makeEmptyFolderAvailableOffline() {
        val folder = "folder1"
        FilesTabRobot
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
    @Scenario(2, isPhotos = true)
    fun makeMultiplePhotosAvailableOffline() {
        val firstImage = ImageName.Yesterday
        val secondImage = ImageName.Now

        FilesTabRobot
            .clickPhotosTab()
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
    @Scenario(4)
    fun makeFolderWithFileInsideAvailableOffline() {
        val folder = "sharedFolder"
        val file = "sharedChild.html"
        FilesTabRobot
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
    @Scenario(4)
    fun makeFolderAndChildFileAvailableOfflineSeparately() {
        val folder = "sharedFolder"
        val file = "sharedChild.html"
        FilesTabRobot
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
}
