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
package me.proton.android.drive.ui.test.flow.creatingFolder

import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.rules.UserLoginRule
import me.proton.android.drive.ui.rules.WelcomeScreenRule
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.toolkits.getRandomString
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.core.test.quark.data.User
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import me.proton.core.drive.i18n.R as I18N

@RunWith(Parameterized::class)
class CreatingFolderFlowErrorTest(
    private val folderName: String,
    private val errorMessage: String,
    @Suppress("unused") private val friendlyName: String
) : BaseTest() {

    private val user
        get() = User(
            dataSetScenario = "2",
            name = "proton_drive_${getRandomString(20)}"
        )

    @get:Rule
    val welcomeScreenRule = WelcomeScreenRule(false)

    @get:Rule
    val userLoginRule = UserLoginRule(testUser = user, shouldSeedUser = true)

    @Test
    fun createFolderError() {
        FilesTabRobot
            .clickPlusButton()
            .clickCreateFolder()
            .typeFolderName(folderName)
            .clickCreate()

        FilesTabRobot
            .verify {
                nodeWithTextDisplayed(errorMessage)
            }
    }

    companion object {
        @get:Parameterized.Parameters(name = "{2}")
        @get:JvmStatic
        val data = listOf(
            arrayOf("folder1", "A file or folder with that name already exists", "alreadyExists"),
            arrayOf(
                ".",
                StringUtils.stringFromResource(I18N.string.common_error_name_periods),
                "forbiddenChar"
            ),
            arrayOf(
                getRandomString(256),
                StringUtils.stringFromResource(I18N.string.common_error_name_too_long, 255),
                "tooLongFilename"
            ),
        )
    }
}
