/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.cross.platform

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.PhotosTabRobot
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class UpdateTest : ConfigurableTest() {

    @Test
    @TestId("move-file")
    fun moveFile() {
        val fileName = location.removePrefix("/")
        val targetName = target.removePrefix("/")

        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(fileName)
            .clickMoreOnItem(fileName)
            .clickMove()
            .clickOnFolderToMove(targetName)
            .clickMoveToFolder(targetName)
            .verify {
                nodeWithTextDisplayed(I18N.string.file_operation_moving_file_successful)
            }
    }

    @Test
    @TestId("move-folder")
    fun moveFolder() {
        val folderName = location.removePrefix("/")
        val targetName = target.removePrefix("/")

        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(folderName)
            .clickMoreOnItem(folderName)
            .clickMove()
            .clickOnFolderToMove(targetName)
            .clickMoveToFolder(targetName)
            .verify {
                nodeWithTextDisplayed(I18N.string.file_operation_moving_folder_successful)
            }
    }

    @Test
    @TestId("rename-file")
    fun renameFile() {
        val fileName = location.removePrefix("/")

        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(fileName)
            .clickMoreOnItem(fileName)
            .clickRename()
            .clearName()
            .typeName(newFileName)
            .clickRename()
            .verify {
                itemIsDisplayed(newFileName)
            }
    }
}
