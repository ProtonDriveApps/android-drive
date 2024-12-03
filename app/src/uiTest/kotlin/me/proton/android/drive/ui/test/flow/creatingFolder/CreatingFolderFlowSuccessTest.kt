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
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.CreateFolderRobot
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.MoveToFolderRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.annotation.SmokeTest
import me.proton.android.drive.utils.getRandomString
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

@HiltAndroidTest
class CreatingFolderFlowSuccessTest : BaseTest() {

    private val randomFolderName get() = getRandomString()

    @Test
    @SmokeTest
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun createFolderViaMoveWindow() {
        val subFolderName = "folder1"
        val newFolderName = getRandomString()

        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(subFolderName)
            .clickMoreOnItem(subFolderName)
            .clickMove()
            .clickAddFolderToRoot()
            .typeFolderName(newFolderName)
            .clickCreate(MoveToFolderRobot)
            .dismissFolderCreateSuccessGrowler(newFolderName, MoveToFolderRobot)
            .scrollToItemWithName(newFolderName) // Can be removed after DRVAND-569 is fixed
            .verify {
                itemIsDisplayed(name = newFolderName)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(value = 2)
    fun createFolderInGridLayout() {
        PhotosTabRobot
            .clickFilesTab()
            .clickLayoutSwitcher()
            .clickPlusButton()
            .clickCreateFolder()

        createFolder(randomFolderName)
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(value = 2)
    fun createAFolderViaSubFolderPlusButton() {
        val subFolderName = "folder1"

        PhotosTabRobot
            .clickFilesTab()
            .clickOnFolder(subFolderName)
            .clickPlusButton()
            .clickCreateFolder()

        createFolder(randomFolderName)
    }

    private fun createFolder(folderName: String) {
        CreateFolderRobot
            .typeFolderName(folderName)
            .clickCreate(FilesTabRobot)
            .dismissFolderCreateSuccessGrowler(folderName, FilesTabRobot)
            .verify {
                itemIsDisplayed(name = folderName)
            }
    }
}
