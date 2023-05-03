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
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import me.proton.core.drive.i18n.R as I18N

@RunWith(AndroidJUnit4::class)
class UploadFlowTest : BaseTest() {
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
    fun uploadEmptyFileWithPlusButton() {
        val file = externalFilesRule.createFile("empty.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFunction {
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
        }

        FilesTabRobot
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(1, StringUtils.stringFromResource(I18N.string.title_my_files))
                itemWithTextDisplayed("empty.txt")
            }
    }
    @Test
    fun uploadEmptyFileWithAddFilesButton() {
        val file = externalFilesRule.createFile("empty.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFunction {
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
        }

        FilesTabRobot
            .clickAddFilesButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(1, StringUtils.stringFromResource(I18N.string.title_my_files))
                itemWithTextDisplayed("empty.txt")
            }
    }

    @Test
    fun cancelFileUpload() {
        val file = externalFilesRule.createFile("cancel.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFunction {
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
        }

        FilesTabRobot
            .clickPlusButton()
            .clickUploadAFile()
            .clickCancelUpload()
            .verify {
                itemWithTextDoesNotExist("cancel.txt")
            }
    }

    @Test
    fun upload4MBFile() {
        val file = externalFilesRule.createFile("4MB.txt", 4 * 1024 * 1024)

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFunction {
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
        }

        FilesTabRobot
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(1, StringUtils.stringFromResource(I18N.string.title_my_files))
                assertStageWaiting()
                assertStageEncrypting()
                assertStageUploading()
                assertStageUploadedProgress(0)
                assertStageUploadedProgress(100)
                itemWithTextDisplayed("4MB.txt")
            }
    }

    @Test
    fun uploadMultipleFiles() {
        val file1 = externalFilesRule.createFile("file1.txt")
        val file2 = externalFilesRule.createFile("file2.txt")
        val file3 = externalFilesRule.createFile("file3.txt")
        val files = listOf(file1, file2, file3)

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFunction {
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().apply {
                val items = files.map { file ->
                    ClipData.Item(Uri.fromFile(file))
                }
                clipData = ClipData(
                    ClipDescription(
                        "", files.map { "text/plain" }.toTypedArray()
                    ),
                    items.first()
                ).also { clipData ->
                    (items - items.first()).forEach(clipData::addItem)
                }
            })
        }

        FilesTabRobot
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(3, StringUtils.stringFromResource(I18N.string.title_my_files))
                itemWithTextDisplayed("file1.txt")
                itemWithTextDisplayed("file2.txt")
                itemWithTextDisplayed("file3.txt")
            }
    }

}