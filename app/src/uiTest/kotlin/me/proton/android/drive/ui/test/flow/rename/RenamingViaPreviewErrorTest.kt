/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.ui.test.flow.rename

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.BackendRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.PreviewRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import org.junit.Test

@HiltAndroidTest
class RenamingViaPreviewErrorTest : AuthenticatedBaseTest() {

    @Test
    @Scenario(1)
    fun renameViaPreviewToAlreadyExistingName() {
        val parent = "folder1"
        val file = "presentation.pdf"
        val renamed = "image.jpg"

        PhotosTabRobot
            .clickFilesTab()
            .clickOnFolder(parent)
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithTextDisplayed("1 / 1")
            }

        PreviewRobot
            .clickOnContextualButton()
            .clickRename()
            .clearName()
            .typeName(renamed)
            .clickRename(PreviewRobot)
            .verify {
                nodeWithTextDisplayed(BackendRobot.nameAlreadyExist)
            }
    }
}
