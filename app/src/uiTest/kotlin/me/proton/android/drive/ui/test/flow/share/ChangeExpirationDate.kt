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
import me.proton.android.drive.ui.extension.tomorrow
import me.proton.android.drive.ui.robot.FileFolderOptionsRobot
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.ShareRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

typealias ClickToShareAction = FileFolderOptionsRobot.() -> ShareRobot

@HiltAndroidTest
@RunWith(Parameterized::class)
class ChangeExpirationDate(
    private val fileName: String,
    private val clickToShareAction: ClickToShareAction,
    @Suppress("unused") private val testName: String,
) : AuthenticatedBaseTest() {

    @Test
    @Scenario(4)
    fun expirationDate() {
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(fileName)
            .clickMoreOnItem(fileName)
            .clickToShareAction()
            .clickExpirationDateTextField()
            .verify { robotDisplayed() }
            .selectDate(tomorrow)
            .clickOk()
            .clickSave()
            .clickUpdateSuccessfulGrowler()
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
            .scrollToItemWithName(fileName)
            .clickMoreOnItem(fileName)
            .clickManageLink()
            .verify {
                robotDisplayed()
                expirationDateToggleIsOn()
                expirationDateIsShown(tomorrow)
            }
    }

    companion object {
        private val GetLink: ClickToShareAction = { clickGetLink() }
        private val ManageLink: ClickToShareAction = { clickManageLink() }

        @get:Parameterized.Parameters(name = "{2}")
        @get:JvmStatic
        val data = listOf(
            arrayOf("image.jpg", GetLink, "setExpirationDate"),
            arrayOf("expiredSharedFile.jpg", ManageLink, "changeExpirationDateOfExpiredLink")
        )
    }
}
