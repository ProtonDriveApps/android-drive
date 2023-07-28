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

import androidx.test.ext.junit.runners.AndroidJUnit4
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.rules.UserLoginRule
import me.proton.android.drive.ui.rules.WelcomeScreenRule
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.toolkits.getRandomString
import me.proton.core.test.quark.data.User
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoveToTrashScenario2Test : BaseTest() {

    private val user
        get() = User(
            dataSetScenario = "2",
            name = "proton_drive_${getRandomString(12)}"
        )

    private val fileName = "image.jpg"
    private val folderName = "folder1"

    @get:Rule
    val welcomeScreenRule = WelcomeScreenRule(false)

    @get:Rule
    val userLoginRule = UserLoginRule(testUser = user, shouldSeedUser = true)

    @Test
    fun moveAFileToTrash() {
        FilesTabRobot
            .clickMoreOnItem(fileName)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemWithTextDoesNotExist(fileName)
            }

        FilesTabRobot
            .clickSidebarButton()
            .clickTrash()
            .verify {
                itemIsDisplayed(fileName)
            }
    }

    @Test
    fun removeFolderWithFiles() {
        FilesTabRobot
            .clickMoreOnItem(folderName)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemWithTextDoesNotExist(folderName)
            }

        FilesTabRobot
            .clickSidebarButton()
            .clickTrash()
            .verify {
                itemIsDisplayed(folderName)
            }
    }
}
