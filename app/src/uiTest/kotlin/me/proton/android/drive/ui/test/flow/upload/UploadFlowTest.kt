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
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.rules.ExternalFilesRule
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.test.android.instrumented.utils.StringUtils
import org.junit.Rule
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class UploadFlowTest : AuthenticatedBaseTest() {

    @get:Rule
    val intentsTestRule = IntentsRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    @get:Rule
    val externalFilesRule = ExternalFilesRule()

    @Test
    fun uploadEmptyFileWithPlusButton() {
        val file = externalFilesRule.createEmptyFile("empty.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFunction {
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
        }

        FilesTabRobot
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(1, StringUtils.stringFromResource(I18N.string.title_my_files))
                itemIsDisplayed("empty.txt")
            }
    }

    @Test
    fun uploadEmptyFileWithAddFilesButton() {
        val file = externalFilesRule.createEmptyFile("empty.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFunction {
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
        }

        FilesTabRobot
            .clickAddFilesButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(1, StringUtils.stringFromResource(I18N.string.title_my_files))
                itemIsDisplayed("empty.txt")
            }
    }

    @Test
    fun cancelFileUpload() {
        val file = externalFilesRule.create1BFile("cancel.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFunction {
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
        }

        FilesTabRobot
            .clickPlusButton()
            .clickUploadAFile()
            .clickCancelUpload()
            .verify {
                itemIsNotDisplayed("cancel.txt")
            }
    }

    @Test
    fun upload6MBFile() {
        val file = externalFilesRule.createFile("6MB.txt", 6 * 1024 * 1024)

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
                itemIsDisplayed("6MB.txt")
            }
    }

    @Test
    fun uploadMultipleFiles() {
        val file1 = externalFilesRule.create1BFile("file1.txt")
        val file2 = externalFilesRule.create1BFile("file2.txt")
        val file3 = externalFilesRule.create1BFile("file3.txt")
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
            }
            .verify {
                itemIsDisplayed("file1.txt")
                itemIsDisplayed("file2.txt")
                itemIsDisplayed("file3.txt")
            }
    }
}
