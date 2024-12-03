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
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.test.rule.annotation.PrepareUser
import me.proton.core.util.kotlin.random
import org.junit.Test

@HiltAndroidTest
class RemoveAccessFlowTest : BaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    @PrepareUser(withTag = "sharingWithUser")
    @Scenario(forTag = "main", value = 6, sharedWithUserTag = "sharingWithUser")
    fun removeInvitation() {
        val sharingUser = protonRule.testDataRule.preparedUsers["sharingWithUser"]!!
        val file = "newShare.txt"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickManageAccess()
            .verify {
                robotDisplayed()
            }
            .clickInvitation(sharingUser.email)
            .clickRemoveAccess()
            .verify {
                robotDisplayed()
                assertNotSharedWith(sharingUser.email)
            }
            .clickBack(FilesTabRobot)
            .verify { itemIsDisplayed(file, isSharedWithUsers = false) }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun removeExternalInvitation() {
        val file = "image.jpg"
        val email = "external_${String.random()}@mail.com"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickShare()
            .verify {
                robotDisplayed()
                assertShareFile(file)
            }
            .typeEmail(email)
            .clickSend()
            .verify {
                dismissInvitationSent(1)
            }
        FilesTabRobot
            .verify { robotDisplayed() }
            .clickMoreOnItem(file)
            .clickManageAccess()
            .clickInvitation(email)
            .clickRemoveAccess()
            .verify {
                robotDisplayed()
                assertNotSharedWith(email)
            }
            .clickBack(FilesTabRobot)
            .verify { itemIsDisplayed(file, isSharedWithUsers = false) }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @PrepareUser(withTag = "sharingWithUser")
    @Scenario(forTag = "main", value = 6, sharedWithUserTag = "sharingWithUser")
    fun removeMember() {
        val sharingUser = protonRule.testDataRule.preparedUsers["sharingWithUser"]!!
        val folder = "legacyShare"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickManageAccess()
            .verify {
                robotDisplayed()
            }
            .clickMember(sharingUser.email)
            .clickRemoveAccess()
            .verify {
                robotDisplayed()
                assertNotSharedWith(sharingUser.email)
            }
            .clickBack(FilesTabRobot)
            .verify { itemIsDisplayed(folder, isSharedWithUsers = false) }
    }
}
