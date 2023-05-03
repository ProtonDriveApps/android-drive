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

package me.proton.android.drive.ui.test.flow.share

import me.proton.android.drive.ui.robot.FileFolderOptionsRobot
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.ShareRobot
import me.proton.android.drive.ui.rules.UserLoginRule
import me.proton.android.drive.ui.rules.WelcomeScreenRule
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.toolkits.getRandomString
import me.proton.core.test.quark.data.User
import org.junit.Rule
import org.junit.Test

class SetCustomPasswordFlowTest : BaseTest() {
    private val user
        get() = User(
            dataSetScenario = "4",
            name = "proton_drive_${getRandomString(20)}"
        )

    @get:Rule
    val welcomeScreenRule = WelcomeScreenRule(false)

    @get:Rule
    val userLoginRule = UserLoginRule(testUser = user, shouldSeedUser = true)

    @Test
    fun deleteCustomPassword() {
        val file = FOLDER_SHARED_WITH_PASSWORD
        showShareViaLinkScreen(file) {
            clickManageLink()
        }

        ShareRobot
            .clickPasswordToggle()
            .clickSave()
            .verify {
                shareUpdateSuccessWasShown()
                saveDoesNotExists()
            }
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickManageLink()
            .verify {
                robotDisplayed()
                passwordToggleIsOff()
            }
    }

    @Test
    fun setCustomPassword() {
        val file = FILE_SHARED
        showShareViaLinkScreen(file) {
            clickManageLink()
        }

        ShareRobot
            .verify { publicAccessibilityDescriptionWasShown() }
            .typePassword(FILE_SHARE_PASSWORD)
            .verify { passwordProtectedAccessibilityDescriptionWasShown() }
            .clickSave()
            .verify {
                shareUpdateSuccessWasShown()
                saveDoesNotExists()
            }
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickManageLink()
            .verify {
                robotDisplayed()
                passwordToggleIsOn()
            }
    }

    @Test
    fun copyPasswordAndVerifyAllowedLength() {
        val file = FILE_SHARED_WITH_PASSWORD
        showShareViaLinkScreen(file) {
            clickManageLink()
        }

        ShareRobot
            .clickCopyPassword()
            .verify { passwordCopiedToClipboardWasShown() }
            .clearPassword()
            .clickSave()
            .verify { passwordLengthErrorWasShown(uiTestHelper.configurationProvider.maxSharedLinkPasswordLength) }
    }

    @Test
    fun discardPasswordChanges() {
        val file = FILE
        showShareViaLinkScreen(file) {
            clickGetLink()
        }

        ShareRobot
            .typePassword(FILE_SHARE_PASSWORD)
            .clickBack(ShareRobot.DiscardChanges)
            .verify { robotDisplayed() }
            .clickDiscard()
            .verify { robotDisplayed() }
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickManageLink()
            .verify {
                robotDisplayed()
                passwordToggleIsOff()
            }
    }

    private fun showShareViaLinkScreen(file: String, clickToShareRobot: FileFolderOptionsRobot.() -> ShareRobot) {
        FilesTabRobot
            .verify { robotDisplayed() }
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickToShareRobot()
            .verify { robotDisplayed() }
    }

    companion object {
        private const val FILE_SHARED = "shared.jpg"
        private const val FILE_SHARED_WITH_PASSWORD = "sharedWithPassword.jpg"
        private const val FOLDER_SHARED_WITH_PASSWORD = "sharedFolderWithPassword"
        private const val FILE = "image.jpg"
        private const val FILE_SHARE_PASSWORD = "1234"
    }
}
