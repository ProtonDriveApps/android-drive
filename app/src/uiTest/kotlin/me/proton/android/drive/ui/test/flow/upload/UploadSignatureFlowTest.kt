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

package me.proton.android.drive.ui.test.flow.upload

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.initializer.MainInitializer
import me.proton.android.drive.ui.MainActivity
import me.proton.android.drive.ui.extension.createFusionComposeRule
import me.proton.android.drive.ui.extension.mainUserId
import me.proton.android.drive.ui.extension.respondWithFile
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.SharedByMeRobot
import me.proton.android.drive.ui.rules.ExternalFilesRule
import me.proton.android.drive.ui.rules.OverlayRule
import me.proton.android.drive.ui.rules.SlowTestRule
import me.proton.android.drive.ui.test.AbstractBaseTest
import me.proton.android.drive.utils.getRandomString
import me.proton.android.drive.utils.replaceEmailPrefix
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.i18n.R
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.v2.command.populate
import me.proton.core.test.quark.v2.command.userCreateAddress
import me.proton.core.test.quark.v2.command.volumeCreate
import me.proton.core.test.rule.ProtonRule
import me.proton.core.test.rule.annotation.PrepareUser
import me.proton.core.test.rule.annotation.mapToUser
import me.proton.core.test.rule.extension.protonRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class UploadSignatureFlowTest : AbstractBaseTest() {

    private val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        *externalStoragePermissions.toTypedArray()
    )

    val protonRule: ProtonRule = protonRule(
        annotationTestData = driveTestDataRule.scenarioAnnotationTestData,
        additionalRules = linkedSetOf(
            IntentsRule(),
            SlowTestRule(),
            OverlayRule(this),
        ),
        beforeHilt = {
            configureFusion()
        },
        afterHilt = {
            MainInitializer.init(it.targetContext)
        },
        logoutBefore = true
    )

    override val mainUserId: UserId get() = protonRule.mainUserId

    @get:Rule(order = 2)
    val ruleChain: RuleChain = RuleChain
        .outerRule(permissionRule)
        .around(protonRule)

    @get:Rule(order = 3)
    val externalFilesRule = ExternalFilesRule()

    @get:Rule(order = 4)
    val composeContentTestRule = createFusionComposeRule()

    @Before
    fun setUp() {
        val file = externalFilesRule.create1BFile("file.txt")
        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFile(file)
    }

    @Test
    @PrepareUser
    fun uploadWithContextShareAddress() {
        val user = protonRule.testDataRule.mainTestUser!!
        val primaryName = getRandomString(10)
        val primaryEmail = user.email.replaceEmailPrefix(primaryName)

        quarkRule.quarkCommands.volumeCreate(user.mapToUser())

        protonRule.testDataRule.quarkCommand.userCreateAddress(
            decryptedUserId = user.decryptedUserId,
            password = user.password,
            email = primaryEmail,
            isPrimary = true
        )
        loginTestHelper.login(user.name, user.password)
        ActivityScenario.launch(MainActivity::class.java)

        PhotosTabRobot
            .waitUntilLoaded()

        PhotosTabRobot
            .openSidebarBySwipe()
            .verify {
                assertDisplayNameIsDisplayed(
                    primaryName
                )
            }
            .closeSidebarBySwipe(PhotosTabRobot)
            .clickFilesTab()
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                dismissFilesBeingUploaded(
                    1,
                    StringUtils.stringFromResource(R.string.title_my_files)
                )
            }
            .clickMoreOnItem("file.txt")
            .clickFileDetails()
            .verify {
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_uploaded_by_entry),
                    value = user.email,
                )
            }
    }

    @Test
    @PrepareUser(withTag = "main")
    @PrepareUser(withTag = "testUserTo")
    fun uploadWithMembershipShareAddress() {
        val testUserFrom = protonRule.testDataRule.mainTestUser!!
        val testUserTo = protonRule.testDataRule.preparedUsers["testUserTo"]!!

        quarkRule.quarkCommands.volumeCreate(testUserTo.mapToUser())

        val invitationName = "invitation${getRandomString(5)}"
        val invitationEmail = testUserTo.email.replaceEmailPrefix(invitationName)
        val primaryName = "primary${getRandomString(5)}"
        val primaryEmail = testUserTo.email.replaceEmailPrefix(primaryName)

        createPrimaryAddress(testUserTo.mapToUser(), invitationEmail)

        quarkRule.quarkCommands.populate(
            user = testUserFrom.mapToUser(),
            scenario = 6,
            sharingUser = testUserTo.mapToUser()
        )

        createPrimaryAddress(testUserTo.mapToUser(), primaryEmail)

        loginTestHelper.login(testUserTo.name, testUserTo.password)
        ActivityScenario.launch(MainActivity::class.java)

        PhotosTabRobot
            .waitUntilLoaded()

        val folder = "ReadWriteFolder"
        PhotosTabRobot
            .openSidebarBySwipe()
            .verify {
                assertDisplayNameIsDisplayed(primaryName)
            }
            .closeSidebarBySwipe(PhotosTabRobot)
            .clickSharedTab()
            .clickSharedWithMeTab()
            .clickOnFolder(folder)
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                dismissFilesBeingUploaded(1, folder)
                assertStageUploadedProgress(100)
                // remove when events are implemented
                Thread.sleep(5_000)
                pullToRefresh(SharedByMeRobot)
            }
            .clickMoreOnItem("file.txt")
            .clickFileDetails()
            .verify {
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_uploaded_by_entry),
                    value = invitationEmail,
                )
            }
    }

    private fun createPrimaryAddress(user: User, primaryEmail: String) {
        protonRule.testDataRule.quarkCommand.userCreateAddress(
            decryptedUserId = user.decryptedUserId,
            password = user.password,
            email = primaryEmail,
            isPrimary = true
        )
    }
}
