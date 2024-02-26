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
import me.proton.android.drive.ui.robot.MoveToFolderRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.android.drive.ui.test.SmokeTest
import me.proton.android.drive.utils.getRandomString
import org.junit.Test

@HiltAndroidTest
class CreatingFolderFlowSuccessTest : AuthenticatedBaseTest() {

    private val randomFolderName get() = getRandomString()

    @Test
    @Scenario(2)
    @SmokeTest
    fun createFolderViaMoveWindow() {
        val subFolderName = "folder1"
        val newFolderName = getRandomString()

        FilesTabRobot
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
    @Scenario(2)
    fun createFolderInGridLayout() {
        FilesTabRobot
            .clickLayoutSwitcher()
            .clickPlusButton()
            .clickCreateFolder()

        createFolder(randomFolderName)
    }

    @Test
    @Scenario(2)
    fun createAFolderViaSubFolderPlusButton() {
        val subFolderName = "folder1"

        FilesTabRobot
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
