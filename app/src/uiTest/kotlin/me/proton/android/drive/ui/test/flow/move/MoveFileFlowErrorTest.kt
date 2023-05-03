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

package me.proton.android.drive.ui.test.flow.move

import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.rules.UserLoginRule
import me.proton.android.drive.ui.rules.WelcomeScreenRule
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.toolkits.getRandomString
import me.proton.core.test.quark.data.User
import org.junit.Rule
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

class MoveFileFlowErrorTest: BaseTest() {

    private val user
        get() = User(
            dataSetScenario = "4",
            name = "proton_drive_${getRandomString(20)}"
        )

    @get:Rule
    val welcomeScreenRule = WelcomeScreenRule(false)

    @get:Rule
    val userLoginRule = UserLoginRule(testUser = user)

    @Test
    fun moveFileWhereSameNameFileAlreadyExists() {
        val file = "sharedChild.html"
        val folderSource = "expiredSharedFolder"
        val folderDestination = "sharedFolder"
        FilesTabRobot
            .scrollToItemWithName(folderSource)
            .clickOnFolder(folderSource)
            .verify {
                robotDisplayed()
            }
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickMove()
            .clickBackFromFolder(folderSource)
            .scrollToItemWithName(folderDestination)
            .clickOnFolder(folderDestination)
            .clickMoveToFolder(folderDestination)
            .verify {
                nodeWithTextDisplayed(I18N.string.file_operation_error_file_already_exists_at_destination)
            }
    }
}
