/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.ui.test.flow.album

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.FeatureFlag
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.AlbumRobot
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.FilesTabRobot.robotDisplayed
import me.proton.android.drive.ui.robot.ManageAccessRobot.assertInvitedWithEditor
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.ShareUserRobot
import me.proton.android.drive.ui.robot.ShareUserRobot.assertShareFile
import me.proton.android.drive.ui.robot.ShareUserRobot.dismissInvitationSent
import me.proton.android.drive.ui.robot.ShareUserRobot.robotDisplayed
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_ALBUMS
import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.v2.command.userCreate
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class SharingUserAlbumFlowTest : BaseTest() {

    private lateinit var email: String

    @Before
    fun setup() {
        email = requireNotNull(quarkRule.quarkCommands.userCreate(User()).email)
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun shareAlbumWithInternalUserAsViewer() {
        val album = "album-for-photos"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .clickOnAlbum(album)
            .clickOnShare()
            .verify {
                robotDisplayed()
                assertShareFile(album)
            }
            .typeEmail(email)
            .clickOnEditorPermission()
            .clickOnViewerPermission()
            .typeMessage("Hello! You can view this file")
            .clickSend()
            .verify {
                dismissInvitationSent(1)
            }
        AlbumRobot
            .clickOnMoreButton()
            .clickManageAccess()
            .verify {
                assertInvitedWithViewer(email)
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 10, sharedWithUserTag = "sharingUser")
    @FeatureFlag(DRIVE_ALBUMS, ENABLED)
    fun shareAlbumWithInternalUserAsEditor() {
        val album = "album-for-photos"
        PhotosTabRobot
            .clickOnAlbumsTitleTab()
            .clickOnAlbum(album)
            .clickOnMoreButton()
            .clickShare()
            .verify {
                robotDisplayed()
                assertShareFile(album)
            }
            .typeEmail(email)
            .clickSend()
            .verify {
                dismissInvitationSent(1)
            }
        AlbumRobot
            .clickOnMoreButton()
            .clickManageAccess()
            .verify {
                assertInvitedWithEditor(email)
            }
    }
}
