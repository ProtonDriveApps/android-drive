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
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import org.junit.Test

@HiltAndroidTest
class InvitationPermissionsFlowTest : AuthenticatedBaseTest() {

    @Test
    @Scenario(6, withSharingUser = true)
    fun updatePermissionsToEditor() {
        val file = "newShare.txt"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickManageAccess()
            .verify {
                robotDisplayed()
            }
            .clickInvitation(userLoginRule.sharingUser.email)
            .clickEditor()
            .verify {
                robotDisplayed()
                assertSharedWithEditor(userLoginRule.sharingUser.email)
            }
    }

    @Test
    @Scenario(6, withSharingUser = true)
    fun updatePermissionsToViewer() {
        val file = "newShare.txt"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickManageAccess()
            .verify {
                robotDisplayed()
            }
            .clickInvitation(userLoginRule.sharingUser.email)
            .clickEditor()
            .clickInvitation(userLoginRule.sharingUser.email)
            .clickViewer()
            .verify {
                robotDisplayed()
                assertSharedWithViewer(userLoginRule.sharingUser.email)
            }
    }
}
