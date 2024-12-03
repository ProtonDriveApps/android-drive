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

package me.proton.android.drive.ui.test.flow.share.user

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.ManageAccessRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.ShareRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

@HiltAndroidTest
class SetCustomPasswordManageAccessFlowTest : BaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 4)
    fun deleteCustomPassword() {
        val file = FOLDER_SHARED_WITH_PASSWORD
        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickManageAccess()
            .clickSettings()
            .verify { robotDisplayed() }
            .clickPasswordToggle()
            .clickSave()
            .clickUpdateSuccessfulGrowler()
            .clickBack(ManageAccessRobot)
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickManageAccess()
            .clickSettings()
            .verify {
                robotDisplayed()
                passwordToggleIsOff()
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 4)
    fun setCustomPassword() {
        val file = FILE_SHARED
        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickManageAccess()
            .clickSettings()
            .verify { robotDisplayed() }
            .typePassword(FILE_SHARE_PASSWORD)
            .clickPasswordToggle()
            .clickSave()
            .clickUpdateSuccessfulGrowler()
            .clickBack(ManageAccessRobot)
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickManageAccess()
            .verify { assertLinkIsShareWithAnyonePasswordProtected() }
            .clickSettings()
            .verify {
                robotDisplayed()
                passwordToggleIsOn()
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 4)
    fun discardPasswordChanges() {
        val file = FILE
        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickManageAccess()
            .clickAllowToAnyone()
            .clickSettings()
            .verify { robotDisplayed() }
            .typePassword(FILE_SHARE_PASSWORD)
            .clickPasswordToggle()
            .clickBack(ShareRobot.DiscardChanges)
            .verify { robotDisplayed() }
            .clickDiscard(ManageAccessRobot)
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickManageAccess()
            .clickSettings()
            .verify {
                robotDisplayed()
                passwordToggleIsOff()
            }
    }

    companion object {
        private const val FILE_SHARED = "shared.jpg"
        private const val FOLDER_SHARED_WITH_PASSWORD = "sharedFolderWithPassword"
        private const val FILE = "image.jpg"
        private const val FILE_SHARE_PASSWORD = "1234"
    }
}
