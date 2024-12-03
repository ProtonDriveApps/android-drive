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
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.data.ImageName
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.drive.files.presentation.extension.LayoutType.Grid
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

@HiltAndroidTest
class MoveToTrashTests : BaseTest() {

    private val fileName = "image.jpg"
    private val folderName = "folder1"

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 1, sharedWithUserTag = "sharingUser")
    fun removeEmptyFolderInGrid() {
        val folder1 = "folder1"
        val folder4 = "folder4"
        val folder6 = "folder6"
        val folder7 = "folder7"
        val emptyFolder = "folder8"

        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .clickLayoutSwitcher()
            .clickOnFolder(folder1, Grid)
            .clickOnFolder(folder4, Grid)
            .clickOnFolder(folder6, Grid)
            .clickOnFolder(folder7, Grid)
            .verify {
                itemIsDisplayed(emptyFolder, Grid)
            }
            .clickMoreOnItem(emptyFolder)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemIsNotDisplayed(emptyFolder)
            }
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                robotDisplayed()
                itemIsDisplayed(emptyFolder, layoutType = Grid)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    fun moveAFileAndPhotoToTrash() {
        PhotosTabRobot.waitUntilLoaded()
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(fileName)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemIsNotDisplayed(fileName)
            }
            .clickPhotosTab()
            .longClickOnPhoto(ImageName.Main.fileName)
            .clickOptions()
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, PhotosTabRobot)
            .verify {
                itemIsNotDisplayed(ImageName.Main.fileName)
            }

        PhotosTabRobot
            .clickFilesTab()
            .clickSidebarButton()
            .clickTrash()
            .verify {
                itemIsDisplayed(fileName)
                itemIsDisplayed(ImageName.Main.fileName)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 9)
    fun removeAnonymousFile() {
        val filename = "anonymous-file"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(filename)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemIsNotDisplayed(filename)
            }

        FilesTabRobot
            .clickFilesTab()
            .clickSidebarButton()
            .clickTrash()
            .verify {
                itemIsDisplayed(filename)
            }
    }
}
