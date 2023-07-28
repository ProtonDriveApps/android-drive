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

package me.proton.android.drive.ui.test.flow.share

import me.proton.android.drive.ui.extension.tomorrow
import me.proton.android.drive.ui.robot.FileFolderOptionsRobot
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.ShareRobot
import me.proton.android.drive.ui.rules.UserLoginRule
import me.proton.android.drive.ui.rules.WelcomeScreenRule
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.toolkits.getRandomString
import me.proton.core.test.quark.data.User
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

typealias ClickToShareAction = FileFolderOptionsRobot.() -> ShareRobot

@RunWith(Parameterized::class)
class ChangeExpirationDate(
    private val fileName: String,
    private val clickToShareAction: ClickToShareAction,
    @Suppress("unused") private val testName: String,
) : BaseTest() {

    private val user
        get() = User(
            dataSetScenario = "4",
            name = "proton_drive_${getRandomString(20)}"
        )

    @get:Rule
    val welcomeScreenRule = WelcomeScreenRule(false)

    @get:Rule
    val userLoginRule = UserLoginRule(testUser = user, shouldSeedUser = true)

    @Test
    fun expirationDate() {
        FilesTabRobot
            .scrollToItemWithName(fileName)
            .clickMoreOnItem(fileName)
            .clickToShareAction()
            .clickExpirationDateTextField()
            .selectDate(tomorrow)
            .clickOk()
            .clickSave()
            .verify {
                shareUpdateSuccessWasShown()
                saveDoesNotExists()
            }
            .clickUpdateSuccessfulGrowler()
            .clickBack(FilesTabRobot)
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
        private val ManageLink: ClickToShareAction = { clickGetLink() }

        @get:Parameterized.Parameters(name = "{2}")
        @get:JvmStatic
        val data = listOf(
            arrayOf("image.jpg", GetLink, "setExpirationDate"),
            arrayOf("expiredSharedFile.jpg", ManageLink, "changeExpirationDateOfExpiredLink")
        )
    }
}
