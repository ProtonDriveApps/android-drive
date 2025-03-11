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

package me.proton.android.drive.cross.platform

import android.content.Intent
import android.os.Environment
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.extension.respondWithFile
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.ExternalFilesRule
import me.proton.core.drive.base.domain.extension.KiB
import me.proton.core.drive.files.presentation.extension.ItemType
import me.proton.core.drive.i18n.R
import me.proton.core.test.android.instrumented.utils.StringUtils
import org.junit.Rule
import org.junit.Test
import java.io.File

@HiltAndroidTest
class CreateTest : ConfigurableTest() {

    @get:Rule(order = 3)
    val externalFilesRule = ExternalFilesRule()

    @get:Rule(order = 3)
    val dcimCameraFolder = ExternalFilesRule {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Camera",
        )
    }

    @Test
    @TestId("backup-photos")
    fun backupPhotos() {
        photoNames.forEach { name ->
            dcimCameraFolder.copyFileFromAssets(name)
        }

        PhotosTabRobot
            .enableBackup()
            .verify {
                assertBackupCompleteDisplayed()
                photoNames.forEach { name ->
                    assertPhotoDisplayed(name)
                }
            }
    }

    @Test
    @TestId("create-folder")
    fun createFolder() {
        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickCreateFolder()
            .typeFolderName(folderName)
            .clickCreate(FilesTabRobot)
            .verify {
                itemIsDisplayed(folderName, itemType = ItemType.Folder)
            }
    }

    @Test
    @TestId("upload")
    fun upload() {
        val file = externalFilesRule.createFile(fileName, 1.KiB.value)
            .apply { writeText("xTesting") }

        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFile(file)

        PhotosTabRobot
            .clickFilesTab()
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                assertFilesBeingUploaded(
                    1,
                    StringUtils.stringFromResource(R.string.title_my_files)
                )
                itemIsDisplayed(fileName)
            }
    }
}
