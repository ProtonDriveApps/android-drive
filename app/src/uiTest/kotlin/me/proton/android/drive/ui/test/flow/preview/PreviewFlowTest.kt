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

package me.proton.android.drive.ui.test.flow.preview

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.android.drive.ui.test.SmokeTest
import me.proton.core.drive.files.presentation.extension.LayoutType
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class PreviewFlowTest : AuthenticatedBaseTest() {

    private val parent = "folder1"

    @Test
    @Scenario(1)
    fun previewTextFileInGrid() {
        val file = "example.txt"
        FilesTabRobot.verify { robotDisplayed() }
            .clickLayoutSwitcher()
            .clickOnFolder(parent, LayoutType.Grid)
            .scrollToItemWithName(file)
            .clickOnFile(file, LayoutType.Grid)
            .verify {
                nodeWithTextDisplayed("Hello World !")
            }
    }

    @Test
    @Scenario(1)
    @SmokeTest
    fun previewImageFile() {
        val file = "image.jpg"
        FilesTabRobot.verify { robotDisplayed() }
            .clickOnFolder(parent)
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithContentDescriptionDisplayed(I18N.string.content_description_file_preview)
            }
    }

    @Test
    @Scenario(1)
    fun previewBrokenImageFile() {
        val file = "broken.jpg"
        FilesTabRobot.verify { robotDisplayed() }
            .clickOnFolder(parent)
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithTextDisplayed(I18N.string.common_error_internal)
            }
    }

    @Test
    @Scenario(1)
    fun previewPdfFile() {
        val file = "presentation.pdf"
        FilesTabRobot.verify { robotDisplayed() }
            .clickOnFolder(parent)
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithTextDisplayed("1 / 1")
            }
    }
}
