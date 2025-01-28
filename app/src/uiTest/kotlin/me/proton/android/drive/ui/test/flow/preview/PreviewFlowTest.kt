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

package me.proton.android.drive.ui.test.flow.preview

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.annotation.SmokeTest
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class PreviewFlowTest : BaseTest() {

    private val parent = "folder1"

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 1)
    fun previewTextFileInGrid() {
        val file = "example.txt"
        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .clickLayoutSwitcher()
            .clickOnFolder(parent, LayoutType.Grid)
            .scrollToItemWithName(file)
            .clickOnFile(file, LayoutType.Grid)
            .verify {
                nodeWithTextDisplayed("Hello World !")
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 1)
    @SmokeTest
    fun previewImageFile() {
        val file = "image.jpg"
        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .clickOnFolder(parent)
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithContentDescriptionDisplayed(I18N.string.content_description_file_preview)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 1)
    fun previewBrokenImageFile() {
        val file = "broken.jpg"
        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .clickOnFolder(parent)
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithTextDisplayed(I18N.string.common_error_internal)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 1)
    fun previewPdfFile() {
        val file = "presentation.pdf"
        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .clickOnFolder(parent)
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithTextDisplayed("1 / 1")
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 9)
    fun previewAnonymousTextFile() {
        val file = "anonymous-file"
        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithTextDisplayed("This is an anonymous file")
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 1)
    fun previewTextFileAfterClearingCache() {
        val file = "example.txt"
        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .clickOnFolder(parent)
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithTextDisplayed("Hello World !")
            }
            .clickBack(FilesTabRobot)
            .clickBack(FilesTabRobot)
            .clickSidebarButton()
            .clickSettings()
            .clickToClearLocalCache()
            .dismissLocalCacheClearedSuccessfulGrowler()
            .clickBack(FilesTabRobot)
            .clickOnFolder(parent)
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithTextDisplayed("Hello World !")
            }
    }
}
