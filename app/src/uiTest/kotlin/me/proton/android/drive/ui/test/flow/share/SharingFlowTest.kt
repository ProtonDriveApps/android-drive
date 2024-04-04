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

package me.proton.android.drive.ui.test.flow.share

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.data.ImageName
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import org.junit.Test

@HiltAndroidTest
class SharingFlowTest : AuthenticatedBaseTest() {
    @Test
    @Scenario(2)
    fun shareFile() {
        val file = "image.jpg"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(file)
            .clickGetLink()
            .verifyShareLinkFile(file)
    }

    @Test
    @Scenario(2, isPhotos = true)
    fun sharePhoto() {
        val image = ImageName.Main
        PhotosTabRobot
            .longClickOnPhoto(image.fileName)
            .clickOptions()
            .clickGetLink()
            .verifyShareLinkFile(image.fileName)
    }

    @Test
    @Scenario(2)
    fun shareFolder() {
        val folder = "folder1"
        PhotosTabRobot
            .clickFilesTab()
            .clickMoreOnItem(folder)
            .clickGetLink()
            .verifyShareLinkFolder(folder)
    }
}
