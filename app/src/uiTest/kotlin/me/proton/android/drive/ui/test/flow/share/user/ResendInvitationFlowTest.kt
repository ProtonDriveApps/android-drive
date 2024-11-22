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
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.util.kotlin.random
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class ResendInvitationFlowTest : AuthenticatedBaseTest() {

    @Test
    @Scenario(6, withSharingUser = true)
    fun resendInvitation() {
        val file = "newShare.txt"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickManageAccess()
            .verify {
                robotDisplayed()
            }
            .clickInvitation(userLoginRule.sharingUser.email)
            .clickResendInvitation()
            .verify { nodeWithTextDisplayed(I18N.string.share_via_invitations_resend_invite_success) }
    }

    @Test
    @Scenario(2)
    fun resendExternalInvitation() {
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
            .clickResendInvitation()
            .verify { nodeWithTextDisplayed(I18N.string.share_via_invitations_resend_invite_success) }
    }
}
