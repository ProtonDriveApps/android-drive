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

package me.proton.android.drive.ui.test.flow.creatingFolder

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.CreateFolderRobot
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.android.drive.utils.getRandomString
import org.junit.Test

@HiltAndroidTest
class CreatingFolderEmptyFlowSuccessTest : AuthenticatedBaseTest() {

    private val randomFolderName get() = getRandomString()

    @Test
    fun createAFolderViaPlusButton() {
        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickCreateFolder()

        createFolder(randomFolderName)
    }

    @Test
    fun createChildFolderViaCTAButton() {

        PhotosTabRobot
            .clickFilesTab()
            .clickAddFilesButton()
            .clickCreateFolder()

        createFolder(randomFolderName)
    }

    private fun createFolder(folderName: String) {
        CreateFolderRobot
            .typeFolderName(folderName)
            .clickCreate(FilesTabRobot)
            .dismissFolderCreateSuccessGrowler(folderName, FilesTabRobot)
            .verify {
                itemIsDisplayed(folderName)
            }
    }
}
