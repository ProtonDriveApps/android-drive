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
import me.proton.android.drive.ui.robot.PreviewRobot
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
    fun moveAnonymousFileToTrash() {
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

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun noTrashItemsInTrash() {
        FilesTabRobot
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                robotDisplayed()
                confirmTrashIsEmpty()
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 5)
    fun removeAFolderWithChildFolder() {
        val folderName = "moveFile"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(folderName)
            .clickMoreOnItem(folderName)
            .clickMoveToTrash()
            .verify {
                moveToTrashSuccessGrowlerIsDisplayedWithUndoButton(1)
            }
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemIsNotDisplayed(folderName)
            }
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                robotDisplayed()
                itemIsDisplayed(folderName)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 5)
    fun removeChildFolder() {
        val parentFolder = "moveFile"
        val childFolder = "folder1"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(parentFolder)
            .clickOnFolder(parentFolder)
            .clickMoreOnItem(childFolder)
            .clickMoveToTrash()
            .verify {
                moveToTrashSuccessGrowlerIsDisplayedWithUndoButton(1)
            }
            .verify {
                itemIsNotDisplayed(childFolder)
            }
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                robotDisplayed()
                itemIsDisplayed(childFolder)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 5)
    fun removeMultipleItemsUsingMultiSelection() {
        val parentFolder = "moveFile"
        val file1 = "sameName.txt"
        val file2 = "index.html"
        val file3 = "presentation.pdf"
        val folder = "folder1"

        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(parentFolder)
            .clickOnFolder(parentFolder)
            .clickLayoutSwitcher()
            .scrollToItemWithName(file1)
            .longClickOnItem(file1)
            .scrollToItemWithName(folder)
            .longClickOnItem(folder)
            .scrollToItemWithName(file2)
            .longClickOnItem(file2)
            .scrollToItemWithName(file3)
            .longClickOnItem(file3)
            .clickOptions()
            .verify {
                numberOfItemsSelectedIsSeen(4)
            }
            .clickMoveToTrash()
            .verify {
                moveToTrashSuccessGrowlerIsDisplayedWithUndoButton(4)
            }
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                robotDisplayed()
                itemIsDisplayed(folder)
                itemIsDisplayed(file1)
                itemIsDisplayed(file2)
                itemIsDisplayed(file3)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 5)
    fun moveManyItemsToTrash() {
        val parentFolder = "moveFile"
        val file = "example.txt"
        val folder = "folder1"
        val numOfItems = 11

        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(parentFolder)
            .clickOnFolder(parentFolder)
            .scrollToItemWithName(file)
            .longClickOnItem(file)
            .clickSelectAll()
            .verify {
                navBarShowsNumOfItemsSelected(numOfItems)
            }
            .clickOptions()
            .verify {
                numberOfItemsSelectedIsSeen(numOfItems)
            }
            .clickMoveToTrash()
            .verify {
                moveToTrashSuccessGrowlerIsDisplayedWithUndoButton(numOfItems)
            }
            .verify {
                letsFillThisFolderMessageIsDisplayed()
            }
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                robotDisplayed()
                itemIsDisplayed(folder)
                numberOfItemsInTrash(11)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    fun movePhotoToTrashFromPreview() {
        val dec2022 = ImageName.December2022
        val jan2023 = ImageName.January2023
        val lastMonth = ImageName.LastMonth
        val lastWeek = ImageName.LastWeek
        val yesterday = ImageName.Yesterday
        val main = ImageName.Main
        val now = ImageName.Now

        PhotosTabRobot.waitUntilLoaded()
        PhotosTabRobot
            .clickPhotosTab()
            .longClickOnPhoto(main.fileName)
            .clickOptions()
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, PreviewRobot)
        PhotosTabRobot
            .scrollToEnd()
            .clickOnPhoto(dec2022)
            .verify { assertPreviewIsDisplayed(dec2022.fileName) }
            .clickOnContextualButton()
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, PreviewRobot)
            .verify { assertPreviewIsDisplayed(jan2023.fileName) }
            .clickOnContextualButton()
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, PreviewRobot)
            .verify { assertPreviewIsDisplayed(lastMonth.fileName) }
            .clickOnContextualButton()
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, PreviewRobot)
            .verify { assertPreviewIsDisplayed(lastWeek.fileName) }
        .clickBack(PhotosTabRobot)
            .verify {
                endToEndEncryptedFooterIsSeen()
                assertPhotoCountEquals(3)
                assertPhotoNotDisplayed(dec2022.fileName)
                assertPhotoNotDisplayed(jan2023.fileName)
                assertPhotoNotDisplayed(lastMonth.fileName)
                assertPhotoNotDisplayed(main.fileName)
                assertPhotoDisplayed(now.fileName)
                assertPhotoDisplayed(yesterday.fileName)
                assertPhotoDisplayed(lastWeek.fileName)
            }
            .clickOnPhoto(now)
            .verify { assertPreviewIsDisplayed(now.fileName) }
        PreviewRobot
            .clickOnContextualButton()
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, PreviewRobot)
            .verify { assertPreviewIsDisplayed(yesterday.fileName) }
            .clickOnContextualButton()
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, PreviewRobot)
            .verify { assertPreviewIsDisplayed(lastWeek.fileName) }
            .clickOnContextualButton()
            .clickMoveToTrash()
        PhotosTabRobot
            .verify { confirmPhotosPermissionMessageIsSeen() }
    }
}
