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
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class MoveFileDeepFlowSuccessTest : AuthenticatedBaseTest() {

    @Test
    @Scenario(1)
    fun moveAFileToParent() {
        val file = "file4"
        val folder2 = "folder2"
        val folder5 = "folder5"
        PhotosTabRobot
            .clickFilesTab()
            .clickOnFolder(folder2)
            .clickOnFolder(folder5)
            .clickMoreOnItem(file)
            .clickMove()
            .clickBackFromFolder(folder5)
            .clickMoveToFolder(folder2)
            .verify {
                nodeWithTextDisplayed(I18N.string.file_operation_moving_file_successful)
                robotDisplayed()
            }
            .clickBack(FilesTabRobot)
            .verify {
                itemIsDisplayed(file)
            }
    }
    @Test
    @Scenario(1)
    fun moveFileFromParentFolderToChildFolderOfAnotherFolder() {
        val file = "file4"
        val folder1 = "folder1"
        val folder2 = "folder2"
        val folder5 = "folder5"
        PhotosTabRobot
            .clickFilesTab()
            .clickOnFolder(folder2)
            .clickOnFolder(folder5)
            .clickMoreOnItem(file)
            .clickMove()
            .clickBackFromFolder(folder5)
            .clickBackFromFolder(folder2)
            .clickOnFolderToMove(folder1)
            .clickMoveToFolder(folder1)
            .verify {
                nodeWithTextDisplayed(I18N.string.file_operation_moving_file_successful)
                robotDisplayed()
            }
            .clickBack(FilesTabRobot)
            .clickBack(FilesTabRobot)
            .clickOnFolder(folder1)
            .verify {
                itemIsDisplayed(file)
            }
    }
}
