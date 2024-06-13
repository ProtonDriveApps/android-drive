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

package me.proton.android.drive.ui.test.flow.share

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.FeatureFlag
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_SHARING_INVITATIONS
import org.junit.Test

@HiltAndroidTest
class RemoveAccessFlowTest : AuthenticatedBaseTest() {

    @Test
    @Scenario(6, withSharingUser = true)
    @FeatureFlag(DRIVE_SHARING_INVITATIONS, ENABLED)
    fun removeInvitation() {
        val file = "newShare.txt"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickManageAccess()
            .verify {
                robotDisplayed()
            }
            .clickInvitation(userLoginRule.sharingUser.email)
            .clickRemoveAccess()
            .verify {
                robotDisplayed()
                assertNotSharedWith(userLoginRule.sharingUser.email)
            }
            .clickBack(FilesTabRobot)
            .verify { itemIsDisplayed(file, isSharedWithUsers = false) }
    }

    @Test
    @Scenario(6, withSharingUser = true)
    @FeatureFlag(DRIVE_SHARING_INVITATIONS, ENABLED)
    fun removeMember() {
        val folder = "legacyShare"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickManageAccess()
            .verify {
                robotDisplayed()
            }
            .clickMember(userLoginRule.sharingUser.email)
            .clickRemoveAccess()
            .verify {
                robotDisplayed()
                assertNotSharedWith(userLoginRule.sharingUser.email)
            }
            .clickBack(FilesTabRobot)
            .verify { itemIsDisplayed(folder, isSharedWithUsers = false) }
    }
}
