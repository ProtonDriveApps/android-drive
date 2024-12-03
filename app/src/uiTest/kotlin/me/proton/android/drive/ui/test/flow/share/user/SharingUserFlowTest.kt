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
import me.proton.android.drive.ui.annotation.FeatureFlag
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.data.ImageName
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_SHARING_EXTERNAL_INVITATIONS_DISABLED
import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.v2.command.userCreate
import me.proton.core.test.rule.annotation.PrepareUser
import me.proton.core.util.kotlin.random
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class SharingUserFlowTest : BaseTest() {

    private lateinit var email: String

    @Before
    fun setup() {
        email = requireNotNull(quarkRule.quarkCommands.userCreate(User()).email)
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun shareFileWithInternalUserAsViewer() {
        val file = "image.jpg"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickShare()
            .verify {
                robotDisplayed()
                assertShareFile(file)
            }
            .typeEmail(email)
            .clickOnEditorPermission()
            .clickOnViewerPermission()
            .clickOnSendMessageAndName()
            .typeMessage("Hello! You can view this file")
            .clickSend()
            .verify {
                dismissInvitationSent(1)
            }
        FilesTabRobot
            .verify { robotDisplayed() }
            .clickMoreOnItem(file)
            .clickManageAccess()
            .verify {
                assertInvitedWithViewer(email)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun shareFolderWithInternalUserAsEditor() {
        val folder = "folder1"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickShare()
            .verify {
                robotDisplayed()
                assertShareFile(folder)
            }
            .typeEmail(email)
            .clickSend()
            .verify {
                dismissInvitationSent(1)
            }
        FilesTabRobot
            .verify { robotDisplayed() }
            .clickMoreOnItem(folder)
            .clickManageAccess()
            .verify {
                assertInvitedWithEditor(email)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 9)
    fun shareAnonymousFileWithInternalUserAsEditor() {
        val file = "anonymous-file"
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
            .verify {
                assertInvitedWithEditor(email)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 4)
    fun shareAlreadySharedFileViaManageAccess() {
        val file = "shared.jpg"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickManageAccess()
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
        // TODO event after sharing is implemented
        // continue the test by clicking back and verifying invitation is present
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    fun shareFileWithMyself() {
        val email = protonRule.testDataRule.mainTestUser!!.email
        val file = "image.jpg"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickShare()
            .verify {
                robotDisplayed()
                assertShareFile(file)
            }
            .typeEmail(email)
            .verify {
                assertInvalidEmail(email)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    @FeatureFlag(DRIVE_SHARING_EXTERNAL_INVITATIONS_DISABLED, ENABLED)
    fun sharePhotoWithExternalUserDisallowed() {
        val email = "external_${String.random()}@mail.com"
        val image = ImageName.Main.fileName
        PhotosTabRobot.waitUntilLoaded()
        PhotosTabRobot
            .longClickOnPhoto(image)
            .clickOptions()
            .clickShare()
            .verify {
                robotDisplayed()
                assertShareFile(image)
            }
            .typeEmail(email)
            .verify {
                assertInvalidEmail(email)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    fun sharePhotoWithExternalUserAllowed() {
        val email = "external_${String.random()}@mail.com"
        val image = ImageName.Main.fileName
        PhotosTabRobot.waitUntilLoaded()
        PhotosTabRobot
            .longClickOnPhoto(image)
            .clickOptions()
            .clickShare()
            .verify {
                robotDisplayed()
                assertShareFile(image)
            }
            .typeEmail(email)
            .clickOnSendMessageAndName()
            .typeMessage("Hello! I hope you will receive this invite")
            .clickSend()
            .verify {
                dismissInvitationSent(1)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @PrepareUser(withTag = "secondUser")
    @Scenario(forTag = "main", value = 2)
    fun shareFolderToMultipleUsers() {
        val folder = "folder1"
        val email2 = protonRule.testDataRule.preparedUsers["secondUser"]!!.email
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickShare()
            .verify {
                robotDisplayed()
                assertShareFile(folder)
            }
            .typeEmail(email)
            .verify { assertValidEmail(email) }
            .typeEmail(email2)
            .clickSend()
            .verify {
                dismissInvitationSent(2)
            }
        FilesTabRobot
            .verify { robotDisplayed() }
            .clickMoreOnItem(folder)
            .clickManageAccess()
            .verify {
                assertInvitedWithEditor(email)
                assertInvitedWithEditor(email2)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    @FeatureFlag(FeatureFlagId.DRIVE_SHARING_DISABLED, ENABLED)
    fun killSwitch() {
        val folder = "folder1"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickShare()
            .verify {
                robotDisplayed()
                assertShareFile(folder)
            }
            .typeEmail(email)
            .verify {
                assertSendButtonDisabled()
            }
    }
}
