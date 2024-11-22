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
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.NetworkSimulator
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.drive.files.presentation.extension.SemanticsDownloadState
import org.junit.Test

@HiltAndroidTest
class AvailableOfflineFlowTest : AuthenticatedBaseTest() {

    @Test
    fun emptyAvailableOfflineScreen() {
        PhotosTabRobot
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                assertIsEmpty()
            }
    }

    @Test
    @Scenario(2)
    fun aChildOfADeletedFolderShouldNotBeDisplayedInAvailableOffline() {
        val folder = "folder1"
        val file = "file2"
        PhotosTabRobot
            .clickFilesTab()
            .clickOnFolder(folder)
            .clickMoreOnItem(file)
            .clickMakeAvailableOffline()
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
            .clickBack(FilesTabRobot)
            .clickMoreOnItem(folder)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsNotDisplayed(file)
                assertIsEmpty()
            }
    }

    @Test
    @Scenario(2)
    fun previewItemsWithoutConnection() {
        val file = "image.jpg"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickMakeAvailableOffline()
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }

        NetworkSimulator.disableNetwork()

        FilesTabRobot
            .clickOnFile(file)
            .verify {
                assertPreviewIsDisplayed(file)
            }
    }

    @Test
    @Scenario(2)
    fun newlyAddedFilesToAFolderShouldBeDownloadedInstantly() {
        val file = "image.jpg"
        val folder = "folder1"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickMakeAvailableOffline()
            .clickMoreOnItem(file)
            .clickMove()
            .clickOnFolderToMove(folder)
            .clickMoveToFolder(folder)
            .clickOnFolder(folder)
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
    }
}
