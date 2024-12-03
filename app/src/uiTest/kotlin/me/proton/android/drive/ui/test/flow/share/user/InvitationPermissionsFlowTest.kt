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
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

@HiltAndroidTest
class InvitationPermissionsFlowTest : BaseTest() {

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 6, sharedWithUserTag = "sharingUser")
    fun updatePermissionsToEditor() {
        val sharingUser = protonRule.testDataRule.preparedUsers["sharingUser"]!!
        val file = "newShare.txt"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickManageAccess()
            .verify {
                robotDisplayed()
            }
            .clickInvitation(sharingUser.email)
            .clickEditor()
            .verify {
                robotDisplayed()
                assertSharedWithEditor(sharingUser.email)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 6, sharedWithUserTag = "sharingUser")
    fun updatePermissionsToViewer() {
        val sharingUser = protonRule.testDataRule.preparedUsers["sharingUser"]!!
        val file = "newShare.txt"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickManageAccess()
            .verify {
                robotDisplayed()
            }
            .clickInvitation(sharingUser.email)
            .clickEditor()
            .clickInvitation(sharingUser.email)
            .clickViewer()
            .verify {
                robotDisplayed()
                assertSharedWithViewer(sharingUser.email)
            }
    }
}
