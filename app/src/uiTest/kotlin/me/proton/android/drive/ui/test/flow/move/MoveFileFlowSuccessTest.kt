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

package me.proton.android.drive.ui.test.flow.move

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.drive.files.presentation.extension.ItemType
import me.proton.core.drive.files.presentation.extension.LayoutType
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class MoveFileFlowSuccessTest : AuthenticatedBaseTest() {
    @Test
    @Scenario(4)
    fun moveAFileToRoot() {
        val file = "sharedChild.html"
        val folder = "sharedFolder"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(folder)
            .clickOnFolder(folder)
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickMove()
            .clickBackFromFolder(folder)
            .clickMoveToRoot()
            .verify {
                nodeWithTextDisplayed(I18N.string.file_operation_moving_file_successful)
                robotDisplayed()
            }
            .clickBack(FilesTabRobot)
            .scrollToItemWithName(file)
            .verify {
                itemIsDisplayed(file)
            }
    }

    @Test
    @Scenario(4)
    fun moveRootFileToFolder() {
        val file = "shared.jpg"
        val folder = "folder3"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickMove()
            .scrollToItemWithName(folder)
            .clickOnFolderToMove(folder)
            .clickMoveToFolder(folder)
            .verify {
                nodeWithTextDisplayed(I18N.string.file_operation_moving_file_successful)
                robotDisplayed()
            }
            .scrollToItemWithName(folder)
            .clickOnFolder(folder)
            .verify {
                itemIsDisplayed(file)
            }
    }

    @Test
    @Scenario(4)
    fun moveFileFromRootFolderToAnotherFolder() {
        val file = "shared.jpg"
        val folder = "folder3"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(file)
            .longClickOnItem(file)
            .clickOptions()
            .clickMove()
            .clickOnFolderToMove(folder)
            .clickMoveToFolder(folder)
            .verify {
                nodeWithTextDisplayed(I18N.string.file_operation_moving_file_successful)
            }
        // TODO: Verify that the file is really moved when DRVAND-449 is fixed
    }

    @Test
    @Scenario(4)
    fun moveFilesFromRootFolderToAnotherFolderViaMultiSelection() {
        val file1 = "shared.jpg"
        val file2 = "shared.html"
        val folder = "folder3"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(file1)
            .longClickOnItem(file1)
            .scrollToItemWithName(file2)
            .clickOnItem(file2, LayoutType.List, ItemType.File, FilesTabRobot)
            .clickOptions()
            .clickMove()
            .clickOnFolderToMove(folder)
            .clickMoveToFolder(folder)
            .verify {
                nodeWithTextDisplayed(I18N.string.file_operation_moving_multiple_successful, 2)
            }
            .scrollToItemWithName(folder)
            .clickOnFolder(folder)
            .scrollToItemWithName(file1)
            .verify {
                itemIsDisplayed(file1)
            }
            .scrollToItemWithName(file2)
            .verify {
                itemIsDisplayed(file2)
            }
    }

    @Test
    @Scenario(4)
    // This test might be flaky due to snackbar not showing
    fun undoMoveRootFileToFolder() {
        val file = "shared.jpg"
        val folder = "folder3"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickMove()
            .scrollToItemWithName(folder)
            .clickOnFolderToMove(folder)
            .clickMoveToFolder(folder)
            .verify {
                nodeWithTextDisplayed(I18N.string.file_operation_moving_file_successful)
            }
            // Server sometimes responds with 422 "This file or folder was out of date, move failed."
            // if undo is called too quickly, thus a 3 seconds delay before clicking on Undo
            .clickOnUndo(after = 3.seconds, FilesTabRobot)
            .verify {
                nodeWithTextDisplayed(I18N.string.file_operation_moving_file_successful)
            }
            .scrollToItemWithName(file)
            .verify {
                itemIsDisplayed(file)
            }
    }
}
