/*
 * Copyright (c) 2023 Proton AG.
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

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.rule.GrantPermissionRule
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.rules.ExternalFilesRule
import me.proton.android.drive.ui.rules.UserLoginRule
import me.proton.android.drive.ui.rules.WelcomeScreenRule
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.toolkits.getRandomString
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.core.test.quark.data.User
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import me.proton.core.drive.i18n.R as I18N

@RunWith(Parameterized::class)
class UploadWithThumbnailFlowTest(
    private val fileName: String,
    private val hasThumbnail: () -> Boolean
) : BaseTest() {

    private val user
        get() = User(
            name = "proton_drive_${getRandomString(20)}"
        )

    @get:Rule
    val intentsTestRule = IntentsRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    @get:Rule
    val externalFilesRule = ExternalFilesRule()

    @get:Rule
    val welcomeScreenRule = WelcomeScreenRule(false)

    @get:Rule
    val userLoginRule = UserLoginRule(testUser = user)

    @Test
    fun uploadWithThumbnail() {
        val file = externalFilesRule.copyFileFromAssets(fileName)

        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWithFunction {
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().setData(Uri.fromFile(file))
                )
            }

        FilesTabRobot
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    count = 1,
                    folderName = StringUtils.stringFromResource(I18N.string.title_my_files)
                )
                if (hasThumbnail()) {
                    itemWithThumbnail(fileName)
                } else {
                    imageWithoutThumbnail(fileName)
                }
            }
    }

    companion object {

        private val ALL_VERSIONS = { true }
        private val ABOVE_Q = { Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q }

        @get:Parameterized.Parameters(name = "{0}")
        @get:JvmStatic
        val data = listOf(
            arrayOf("boat.mp3", ABOVE_Q),
            arrayOf("boat.mp4", ALL_VERSIONS),
            arrayOf("boat.jpg", ALL_VERSIONS),
            arrayOf("small.pdf", ALL_VERSIONS),
            arrayOf("small.svg", ALL_VERSIONS),
        )
    }
}
