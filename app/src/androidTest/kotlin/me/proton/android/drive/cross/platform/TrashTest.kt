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

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import org.junit.Test

@HiltAndroidTest
class TrashTest : ConfigurableTest() {

    @Test
    @TestId("restore-file")
    fun restoreFile() {
        val fileName = location.removePrefix("/")

        PhotosTabRobot
            .openSidebarBySwipe()
            .clickTrash()
            .clickMoreOnItem(fileName)
            .clickRestoreTrash()
            .verify {
                itemIsNotDisplayed(fileName)
            }
    }

    @Test
    @TestId("trash-file")
    fun trashFile() {
        val fileName = location.removePrefix("/")

        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(fileName)
            .clickMoreOnItem(fileName)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify { itemIsNotDisplayed(fileName) }
    }
}
