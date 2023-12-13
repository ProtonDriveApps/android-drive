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
class DeletePermanentlyTests: AuthenticatedBaseTest() {
    @Test
    @Scenario(2, isPhotos = true)
    fun deletePhotoAndFolderPermanently() {
        val folder = "folder1"
        val photo = ImageName.Main.fileName

        FilesTabRobot
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
}