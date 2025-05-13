/*
 * Copyright (c) 2024 Proton AG.
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

import androidx.test.core.app.ActivityScenario
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.MainActivity
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.SharedWithMeRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.utils.getRandomString
import me.proton.android.drive.utils.replaceEmailPrefix
import me.proton.core.test.quark.v2.command.populate
import me.proton.core.test.quark.v2.command.userCreateAddress
import me.proton.core.test.rule.annotation.PrepareUser
import me.proton.core.test.rule.annotation.mapToUser
import org.junit.Test

@HiltAndroidTest
class UserInvitationIdFlowTest : BaseTest() {

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 6, sharedWithUserTag = "main")
    fun acceptUserInvitationFileAndPreview() {
        val name = "newShare.txt"
        PhotosTabRobot
            .navigateToSharedWithMeTab()
            .clickUserInvitation(2)
            .clickAccept(name)
            .dismissAcceptSucceed()
            .clickBack(SharedWithMeRobot)
            .scrollToItemWithName(name)
            .clickOnFile(name)
            .verify {
                nodeWithTextDisplayed("Hello World!")
            }
    }

    @Test
    @PrepareUser(withTag = "main")
    @PrepareUser(withTag = "sharingUser")
    fun acceptUserInvitationWithMultipleAddress() {
        val user = protonRule.testDataRule.mainTestUser!!
        val primaryName = getRandomString(10)
        val primaryEmail = user.email.replaceEmailPrefix(primaryName)

        // Create another address before login
        protonRule.testDataRule.quarkCommand.userCreateAddress(
            decryptedUserId = user.decryptedUserId,
            password = user.password,
            email = primaryEmail,
            isPrimary = true
        )

        val sharingUser = requireNotNull(protonRule.testDataRule.preparedUsers["sharingUser"])
        quarkRule.quarkCommands.populate(sharingUser.mapToUser(), scenario = 6, sharingUser = user.mapToUser())

        loginTestHelper.login(user.name, user.password)
        ActivityScenario.launch(MainActivity::class.java)

        val name = "newShare.txt"
        PhotosTabRobot
            .navigateToSharedWithMeTab()
            .clickUserInvitation(2)
            .clickAccept(name)
            .dismissAcceptSucceed()
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 6, sharedWithUserTag = "main")
    fun acceptUserInvitationFolderAndOpen() {
        val name = "newShare"
        PhotosTabRobot
            .navigateToSharedWithMeTab()
            .clickUserInvitation(2)
            .clickAccept(name)
            .dismissAcceptSucceed()
            .clickBack(SharedWithMeRobot)
            .scrollToItemWithName(name)
            .clickOnFolder(name)
            .verify {
                itemIsDisplayed("legacyShareInsideNew.txt")
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 6, sharedWithUserTag = "main")
    fun declineUserInvitation() {
        PhotosTabRobot
            .navigateToSharedWithMeTab()
            .clickUserInvitation(2)
            .clickDecline("newShare")
            .dismissDeclineSucceed()
            .clickBack(SharedWithMeRobot)
            .verify {
                itemIsNotDisplayed("newShare")
            }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 6, sharedWithUserTag = "main")
    fun emptyUserInvitation() {
        PhotosTabRobot
            .navigateToSharedWithMeTab()
            .clickUserInvitation(2)
            .clickDecline("newShare")
            .dismissDeclineSucceed()
            .clickDecline("newShare.txt")
            .dismissDeclineSucceed()
            .verify {
                assertEmpty()
            }
    }

    private fun PhotosTabRobot.navigateToSharedWithMeTab(): SharedWithMeRobot =
        this
            .verify {
                waitUntilLoaded()
                robotDisplayed()
            }
            .clickSharedTab()
            .clickSharedWithMeTab()
}
