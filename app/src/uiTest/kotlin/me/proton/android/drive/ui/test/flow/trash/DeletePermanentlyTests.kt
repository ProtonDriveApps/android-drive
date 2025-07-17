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
import me.proton.android.drive.ui.robot.TrashRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

@HiltAndroidTest
class DeletePermanentlyTests : BaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    fun deletePhotoAndFolderPermanently() {
        val folder = "folder1"
        val photo = ImageName.Main.fileName

        PhotosTabRobot.waitUntilLoaded()
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemIsNotDisplayed(folder)
            }
            .clickPhotosTab()
            .longClickOnPhoto(photo)
            .clickOptions()
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, PhotosTabRobot)
            .clickSidebarButton()
            .clickTrash()
            .verify {
                itemIsDisplayed(folder)
            }
            .clickMoreOnItem(folder)
            .clickDeletePermanently()
            .confirmDelete()
            .dismissDeleteSuccessGrowler(1, TrashRobot)
            .verify {
                itemIsNotDisplayed(folder)
            }
            .clickMoreOnItem(photo)
            .clickDeletePermanently()
            .confirmDelete()
            .verify {
                itemIsNotDisplayed(photo)
                itemIsNotDisplayed(folder)
            }
            .clickBack(FilesTabRobot)
            .verify {
                itemIsNotDisplayed(folder)
            }
            .clickPhotosTab()
            .verify {
                itemIsNotDisplayed(photo)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 4, isPhotos = true, isDevice = true)
    fun emptyTrashDeleteAllItems() {
        val itemsInTrash = arrayOf(
            "emptyTrashedFolder",
            "trashedFolderWithChildren",
            "trashedFile.json",
            "trashedFileInDevice",
            ImageName.Trashed1.fileName,
            ImageName.Trashed2.fileName
        )

        PhotosTabRobot
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                robotDisplayed()
            }
            .openMoreOptions()
            .clickEmptyTrash()
            .confirmEmptyTrash()
            .verify {
                itemIsNotDisplayed(*itemsInTrash)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    fun emptyTrashPhotoDeleteAllItems() {
        val itemsInTrash = arrayOf(
            "trashedFileInAlbumByOtherUser.jpg",
            "trashedFileInAlbum.jpg",
            "trashedFileInStreamAndInAlbum.jpg",
        )

        PhotosTabRobot
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                robotDisplayed()
            }
            .openMoreOptions()
            .clickEmptyTrash()
            .confirmEmptyTrash()
            .verify {
                itemIsNotDisplayed(*itemsInTrash)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    fun deleteChildAndParent() {
        val folder = "folder1"
        val file = "file2"

        PhotosTabRobot.waitUntilLoaded()
        PhotosTabRobot
            .clickFilesTab()
            .clickOnFolder(folder)
            .clickMoreOnItem(file)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemIsNotDisplayed(file)
            }
            .clickBack(FilesTabRobot)
            .clickMoreOnItem(folder)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemIsNotDisplayed(folder)
            }
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                itemIsDisplayed(folder)
                itemIsDisplayed(file)
            }
            .clickMoreOnItem(folder)
            .clickDeletePermanently()
            .confirmDelete()
            .dismissDeleteSuccessGrowler(1, TrashRobot)
            .verify {
                itemIsNotDisplayed(folder, file)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    fun deleteFileAndFolder() {
        val folder = "folder1"
        val file = "image.jpg"

        PhotosTabRobot.waitUntilLoaded()
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemIsNotDisplayed(file)
            }
            .clickMoreOnItem(folder)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemIsNotDisplayed(folder)
            }
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                itemIsDisplayed(folder)
                itemIsDisplayed(file)
            }
            .clickMoreOnItem(folder)
            .clickDeletePermanently()
            .confirmDelete()
            .dismissDeleteSuccessGrowler(1, TrashRobot)
            .verify {
                itemIsNotDisplayed(folder)
            }
            .clickMoreOnItem(file)
            .clickDeletePermanently()
            .confirmDelete()
            .dismissDeleteSuccessGrowler(1, TrashRobot)
            .verify {
                itemIsNotDisplayed(file)
            }
    }


    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    fun deleteAPhotoInAlbum() {
        val image = "trashedFileInAlbum.jpg"
        PhotosTabRobot.waitUntilLoaded()
        PhotosTabRobot
            .clickSidebarButton()
            .clickTrash()
            .clickMoreOnItem(image)
            .clickDeletePermanently()
            .confirmDelete()
            .dismissDeleteSuccessGrowler(1, TrashRobot)
        TrashRobot
            .verify {
                itemIsNotDisplayed(image)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 4, isPhotos = true, isDevice = true)
    fun emptyTrashDeleteAllPhotos() {
        val itemsInTrash = arrayOf(
            "trashedFileInAlbumByOtherUser.jpg",
            "trashedFileInAlbum.jpg",
            "trashedFileInStreamInAlbum.jpg",
        )

        PhotosTabRobot
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                robotDisplayed()
            }
            .openMoreOptions()
            .clickEmptyTrash()
            .confirmEmptyTrash()
            .verify {
                itemIsNotDisplayed(*itemsInTrash)
            }
    }
}
