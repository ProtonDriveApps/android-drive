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

package me.proton.android.drive.ui.test.flow.move

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.drive.files.presentation.extension.LayoutType
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class MoveFolderGridFlowTest : AuthenticatedBaseTest() {

    @Test
    @Scenario(1)
    fun moveAFolderInGrid() {
        val folder = "folder1"
        val folderDestination = "folder2"

        PhotosTabRobot
            .clickFilesTab()
            .clickLayoutSwitcher()
            .scrollToItemWithName(folder)
            .clickMoreOnItem(folder)
            .clickMove()
            .clickOnFolderToMove(folderDestination)
            .clickMoveToFolder(folderDestination)
            .verify {
                nodeWithTextDisplayed(I18N.string.file_operation_moving_folder_successful)
                robotDisplayed()
            }
            .scrollToItemWithName(folderDestination)
            .clickOnFolder(folderDestination, LayoutType.Grid)
            .verify {
                itemIsDisplayed(folder)
            }
    }
}
