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
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.ui.annotation.Quota
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.extension.respondWithFile
import me.proton.android.drive.ui.extension.respondWithFiles
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.StorageFullRobot
import me.proton.android.drive.ui.test.ExternalStorageBaseTest
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class UploadFlowTest : ExternalStorageBaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    fun uploadEmptyFileWithPlusButton() {
        val file = externalFilesRule.createEmptyFile("empty.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFile(file)

        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    1,
                    StringUtils.stringFromResource(I18N.string.title_my_files)
                )
                itemIsDisplayed("empty.txt")
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun uploadEmptyFileWithAddFilesButton() {
        val file = externalFilesRule.createEmptyFile("empty.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFile(file)

        PhotosTabRobot
            .clickFilesTab()
            .clickAddFilesButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    1,
                    StringUtils.stringFromResource(I18N.string.title_my_files)
                )
                itemIsDisplayed("empty.txt")
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun cancelFileUpload() {
        val file = externalFilesRule.create1BFile("cancel.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFile(file)

        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickUploadAFile()
            .clickCancelUpload()
            .verify {
                itemIsNotDisplayed("cancel.txt")
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun upload50MBFile() {
        val file = externalFilesRule.createFile("50MB.txt", 50 * 1024 * 1024)

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFile(file)

        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    1,
                    StringUtils.stringFromResource(I18N.string.title_my_files)
                )
                assertStageWaiting()
                assertStageEncrypting()
                assertStageUploading()
                assertStageUploadedProgress(100, 5.minutes)
                itemIsDisplayed("50MB.txt")
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun uploadMultipleFiles() {
        val file1 = externalFilesRule.create1BFile("file1.txt")
        val file2 = externalFilesRule.create1BFile("file2.txt")
        val file3 = externalFilesRule.create1BFile("file3.txt")
        val files = listOf(file1, file2, file3)

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFiles(files)

        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    3,
                    StringUtils.stringFromResource(I18N.string.title_my_files)
                )
            }
            .verify {
                itemIsDisplayed("file1.txt")
                itemIsDisplayed("file2.txt")
                itemIsDisplayed("file3.txt")
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun uploadAFileInGridSucceeds() {
        val file = externalFilesRule.create1BFile("file.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFile(file)

        PhotosTabRobot
            .clickFilesTab()
            .clickLayoutSwitcher()
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    1,
                    StringUtils.stringFromResource(I18N.string.title_my_files)
                )
            }
            .verify {
                itemIsDisplayed("file.txt")
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun uploadTheSameFileTwice() {
        val file = externalFilesRule.create1BFile("file.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFile(file)

        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    1,
                    StringUtils.stringFromResource(I18N.string.title_my_files)
                )
            }
            .verify {
                itemIsDisplayed("file.txt")
            }
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    1,
                    StringUtils.stringFromResource(I18N.string.title_my_files)
                )
            }
            .verify {
                itemIsDisplayed("file (1).txt")
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", quota = Quota(percentageFull = 99))
    fun notEnoughSpaceWhenUploadOneFileBiggerThanStorage() {

        val file = externalFilesRule.createFile("50MB.txt", 50.MiB.value)

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFile(file)

        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                StorageFullRobot.robotDisplayed()
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun uploadMultipleBatches() {
        val batch200 = (1..200).map { index ->
            externalFilesRule.create1BFile("file$index.txt")
        }
        val batch100 = (201..300).map { index ->
            externalFilesRule.create1BFile("file$index.txt")
        }

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFiles(batch200)

        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    200,
                    StringUtils.stringFromResource(I18N.string.title_my_files)
                )
            }

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFiles(batch100)

        FilesTabRobot
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    100,
                    StringUtils.stringFromResource(I18N.string.title_my_files)
                )
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun navigationIsAllowedDuringFileUpload() {
        val file = externalFilesRule.create1BFile("file.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFile(file)

        PhotosTabRobot
            .clickFilesTab()
            .clickOnFolder("folder1")
            .clickPlusButton()
            .clickUploadAFile()

        // clicking on back to fast is doing nothing
        runBlocking { delay(500) }

        FilesTabRobot
            .clickBack(FilesTabRobot)
            .clickOnFolder("folder1")
            .verify {
                itemIsDisplayed("file.txt")
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun switchLayoutWhileUploading() {
        val file = externalFilesRule.create1BFile("file.txt")

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFile(file)

        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    1,
                    StringUtils.stringFromResource(I18N.string.title_my_files)
                )
            }
            .clickLayoutSwitcher()
            .verify {
                itemIsDisplayed("file.txt")
            }
    }
}
