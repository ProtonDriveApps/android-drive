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

import android.Manifest
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.MainActivity
import me.proton.android.drive.ui.extension.populate
import me.proton.android.drive.ui.extension.respondWithFile
import me.proton.android.drive.ui.extension.userCreatePrimaryAddress
import me.proton.android.drive.ui.extension.volumeCreate
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.SharedByMeRobot
import me.proton.android.drive.ui.rules.ExternalFilesRule
import me.proton.android.drive.ui.test.EmptyBaseTest
import me.proton.android.drive.utils.getRandomString
import me.proton.core.drive.i18n.R
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.v2.command.userCreate
import me.proton.test.fusion.FusionConfig.targetContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class UploadSignatureFlowTest : EmptyBaseTest() {

    @get:Rule
    val intentsTestRule = IntentsRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    @get:Rule
    val externalFilesRule = ExternalFilesRule()

    @Before
    fun setUp() {
        val file = externalFilesRule.create1BFile("file.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFile(file)
    }

    @After
    fun tearDown() {
        loginTestHelper.logoutAll()
    }

    @Test
    fun uploadWithContextShareAddress() {
        val testUser = User(name = "proton_drive_${getRandomString(15)}")
        val user = quarkRule.quarkCommands.userCreate(testUser)

        quarkRule.quarkCommands.volumeCreate(testUser)

        val primaryEmail = user.email!!.replace("proton_drive", "pd")
        val primaryName = user.name!!.replace("proton_drive", "pd")
        quarkRule.quarkCommands.userCreatePrimaryAddress(
            decryptedUserId = user.decryptedUserId,
            password = user.password,
            email = primaryEmail,
        )

        loginTestHelper.login(user.name!!, user.password)
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
                dismissFilesBeingUploaded(1, StringUtils.stringFromResource(R.string.title_my_files))
            }
            .clickMoreOnItem("file.txt")
            .clickFileDetails()
            .verify {
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_uploaded_by_entry),
                    value = user.email!!,
                )
            }
    }

    @Test
    fun uploadWithMembershipShareAddress() {
        val testUserFrom = User(
            name = "proton_drive_from_${getRandomString(15)}",
            dataSetScenario = "6",
        )
        val testUserTo = User(name = "proton_drive_to_${getRandomString(15)}")

        quarkRule.quarkCommands.userCreate(testUserFrom)
        val user2 = quarkRule.quarkCommands.userCreate(testUserTo)

        quarkRule.quarkCommands.volumeCreate(testUserTo)

        val invitationEmail = user2.email!!.replace("proton_drive", "inv")
        quarkRule.quarkCommands.userCreatePrimaryAddress(
            decryptedUserId = user2.decryptedUserId,
            password = user2.password,
            email = invitationEmail,
        )

        quarkRule.quarkCommands.populate(testUserFrom, sharingUser = testUserTo)

        val primaryEmail = user2.email!!.replace("proton_drive", "pd")
        val primaryName = user2.name!!.replace("proton_drive", "pd")
        quarkRule.quarkCommands.userCreatePrimaryAddress(
            decryptedUserId = user2.decryptedUserId,
            password = user2.password,
            email = primaryEmail,
        )

        loginTestHelper.login(user2.name!!, user2.password)

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

}
