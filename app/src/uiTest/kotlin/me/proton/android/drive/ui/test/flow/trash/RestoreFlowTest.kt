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

package me.proton.android.drive.ui.test.flow.trash

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.data.ImageName
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.TrashRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import org.junit.Test

@HiltAndroidTest
class RestoreFlowTest : AuthenticatedBaseTest() {

    @Test
    @Scenario(4)
    fun restoreAnItemFromTrashViaThreeDotsButtonOfTheItem() {
        val fileName = "trashedFile.json"
        PhotosTabRobot
            .clickFilesTab()
            .clickSidebarButton()
            .clickTrash()
            .clickMoreOnItem(fileName)
            .clickRestoreTrash()

        TrashRobot
            .verify {
                itemIsNotDisplayed(fileName)
            }
            .clickBack(FilesTabRobot)
            .verify {
                itemIsDisplayed(fileName)
            }
    }

    @Test
    @Scenario(4)
    fun restoreAFolderWithFilesInside() {
        val folderName = "trashedFolderWithChildren"

        PhotosTabRobot
            .clickFilesTab()
            .clickSidebarButton()
            .clickTrash()
            .clickMoreOnItem(folderName)
            .clickRestoreTrash()

        TrashRobot
            .verify {
                itemIsNotDisplayed(folderName)
            }
            .clickBack(FilesTabRobot)
            .clickOnFolder(folderName)
            .verify {
                itemIsDisplayed("trashedIndex.html")
            }
    }

    @Test
    @Scenario(2)
    fun restoreAnItemWhenParentFolderIsInTrash() {
        val parent = "folder1"
        val child = "file2"
        val restoreErrorMessage =
            "This file or folder is no longer available. A parent folder has been deleted."

        PhotosTabRobot
            .clickFilesTab()
            .clickOnFolder(parent)
            .moveToTrash(child)
            .clickBack(FilesTabRobot)
            .moveToTrash(parent)
            .clickSidebarButton()
            .clickTrash()
            .clickMoreOnItem(child)
            .clickRestoreTrash()
            .verify {
                nodeWithTextDisplayed(restoreErrorMessage)
            }
    }

    @Test
    @Scenario(1)
    fun restoreAChildFolder() {
        val folder1 = "folder1"
        val folder3 = "folder3"
        PhotosTabRobot
            .clickFilesTab()
            .clickOnFolder(folder1)
            .moveToTrash(folder3)
            .clickSidebarButton()
            .clickTrash()
            .clickMoreOnItem(folder3)
            .clickRestoreTrash()

        TrashRobot
            .verify {
                itemIsNotDisplayed(folder3)
            }
            .clickBack(FilesTabRobot)
            .clickOnFolder(folder1)
            .verify {
                itemIsDisplayed(folder3)
            }
    }

    @Test
    @Scenario(2, isPhotos = true)
    fun restoreAPhoto() {
        val item = ImageName.Main.fileName
        PhotosTabRobot
            .longClickOnPhoto(item)
            .clickOptions()
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemIsNotDisplayed(item)
            }
            .clickSidebarButton()
            .clickTrash()
            .clickMoreOnItem(ImageName.Main.fileName)
            .clickRestoreTrash()

        TrashRobot
            .verify {
                itemIsNotDisplayed(item)
            }
            .clickBack(PhotosTabRobot)
            .verify {
                itemIsDisplayed(item)
            }
    }

    private fun FilesTabRobot.moveToTrash(name: String) = clickMoreOnItem(name)
        .clickMoveToTrash()
        .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
        .verify {
            itemIsNotDisplayed(name)
        }
}
