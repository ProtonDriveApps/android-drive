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
import me.proton.android.drive.ui.data.ImageName
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.OfflineRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.drive.files.presentation.extension.SemanticsDownloadState
import org.junit.Test

@HiltAndroidTest
class RemoveAvailableOfflineFlowTest : AuthenticatedBaseTest() {

    @Test
    @Scenario(4)
    fun removeFileFromAvailableOfflineFromMyFile() {
        val file = "shared.jpg"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickMakeAvailableOffline()
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
            .clickMoreOnItem(file)
            .clickRemoveAvailableOffline(FilesTabRobot)
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Null)
            }
    }

    @Test
    @Scenario(4)
    fun removeFileFromAvailableOfflineFromOffline() {
        val file = "shared.jpg"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickMakeAvailableOffline()
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
            .openSidebarBySwipe()
            .clickOffline()
            .clickMoreOnItem(file)
            .clickRemoveAvailableOffline(OfflineRobot)
            .verify {
                itemIsNotDisplayed(file)
                assertIsEmpty()
            }
    }

    @Test
    @Scenario(4)
    fun removeFolderFromAvailableOffline() {
        val folder = "folder1"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickMakeAvailableOffline()
            .verify {
                itemIsDisplayed(folder, downloadState = SemanticsDownloadState.Downloaded)
            }
            .clickMoreOnItem(folder)
            .clickRemoveAvailableOffline(FilesTabRobot)
            .verify {
                itemIsDisplayed(folder, downloadState = SemanticsDownloadState.Null)
            }
            .clickBack(FilesTabRobot)
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsNotDisplayed(folder)
                assertIsEmpty()
            }
    }

    @Test
    @Scenario(4)
    fun separatelyAddedFileAreAvailableOfflineAfterParentIsRemoved() {
        val folder = "sharedFolder"
        val file = "sharedChild.html"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickMakeAvailableOffline()
            .clickOnFolder(folder)
            .clickMoreOnItem(file)
            .clickMakeAvailableOffline()
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
            .clickBack(FilesTabRobot)
            .clickMoreOnItem(folder)
            .clickRemoveAvailableOffline(FilesTabRobot)
            .verify {
                itemIsDisplayed(folder, downloadState = SemanticsDownloadState.Null)
            }
            .clickOnFolder(folder)
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
            .clickBack(FilesTabRobot)
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsNotDisplayed(folder)
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
    }

    @Test
    @Scenario(2, isPhotos = true)
    fun removePhotoFromAvailableOffline() {
        val image = ImageName.Now

        PhotosTabRobot
            .longClickOnPhoto(image.fileName)
            .clickOptions()
            .clickMakeAvailableOffline()
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsDisplayed(image.fileName)
            }
            .clickMoreOnItem(image.fileName)
            .clickRemoveAvailableOffline(OfflineRobot)
            .verify {
                itemIsNotDisplayed(image.fileName)
            }
            .clickBack(PhotosTabRobot)
            .verify {
                assertPhotoDisplayed(image)
            }
    }
}
