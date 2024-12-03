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

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.annotation.Quota
import me.proton.android.drive.ui.robot.LauncherRobot
import me.proton.android.drive.ui.robot.StorageFullRobot
import me.proton.android.drive.ui.robot.UploadToRobot
import me.proton.android.drive.ui.test.ExternalStorageBaseTest
import me.proton.core.drive.base.domain.extension.KiB
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class UploadViaThirdPartyAppFlowTest : ExternalStorageBaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    fun uploadEmptyFileViaThirdPartyApp() {
        val file = externalFilesRule.createEmptyFile("empty.txt")

        LauncherRobot.uploadTo(file)
            .verify {
                robotDisplayed()
                assertEmptyFolder() // TODO: Enable upload button when folder is loaded
            }
            .clickUpload()
            .verify {
                assertFilesBeingUploaded(
                    1,
                    StringUtils.stringFromResource(I18N.string.title_my_files)
                )
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    fun uploadMultipleItemsIntoNewlyCreatedFolder() {
        val files = listOf(
            externalFilesRule.createFile("1.txt", 1.KiB.value),
            externalFilesRule.createFile("2.txt", 2.KiB.value),
            externalFilesRule.createFile("3.txt", 3.KiB.value),
        )

        val folderName = "test"
        LauncherRobot.uploadTo(files)
            .verify {
                robotDisplayed()
                assertEmptyFolder() // TODO: Enable upload button when folder is loaded
            }
            .clickCreateFolder()
            .typeFolderName(folderName)
            .clickCreate(UploadToRobot)
            .dismissFolderCreateSuccessGrowler(folderName, UploadToRobot)
            .clickOnFolder(folderName, goesTo = UploadToRobot)
            .verify {
                assertEmptyFolder() // TODO: Enable upload button when folder is loaded
            }
            .clickUpload()
            .verify {
                assertFilesBeingUploaded(3, folderName)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", quota = Quota(percentageFull = 99))
    fun uploadMultipleItemsWhenStorageIsAlmostFull() {
        val files = listOf(
            externalFilesRule.createFile("1.txt", 10.MiB.value),
            externalFilesRule.createFile("2.txt", 10.MiB.value),
            externalFilesRule.createFile("3.txt", 10.MiB.value),
        )

        LauncherRobot.uploadTo(files)
            .verify {
                robotDisplayed()
                assertEmptyFolder() // TODO: Enable upload button when folder is loaded
            }
            .clickUpload()
            .verify {
                StorageFullRobot.robotDisplayed()
            }
    }
}
