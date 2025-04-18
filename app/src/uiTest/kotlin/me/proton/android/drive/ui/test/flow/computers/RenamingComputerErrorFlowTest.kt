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

package me.proton.android.drive.ui.test.flow.computers

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.RenameRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.utils.getRandomString
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
@RunWith(Parameterized::class)
class RenamingComputerErrorFlowTest(
    private val computerToBeRenamed: String,
    private val newComputerName: String,
    private val errorMessage: String,
    @Suppress("unused") private val friendlyName: String,
) : BaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isDevice = true)
    fun renameComputerWithInvalidName() {
        FilesTabRobot
            .clickComputersTab()
            .scrollToComputer(computerToBeRenamed)
            .clickMoreOnComputer(computerToBeRenamed)
            .clickRename()
            .clearName()
            .typeName(newComputerName)
            .clickRename(RenameRobot)
            .verify {
                nodeWithTextDisplayed(errorMessage)
            }
    }

    companion object {
        private const val MY_DEVICE_1 = "MyDevice1"

        @get:Parameterized.Parameters(name = "{3}")
        @get:JvmStatic
        val data = listOf(
            arrayOf(
                MY_DEVICE_1,
                "",
                StringUtils.stringFromResource(I18N.string.common_error_name_is_blank),
                "Empty device name"
            ),
            arrayOf(
                MY_DEVICE_1,
                " \t ",
                StringUtils.stringFromResource(I18N.string.common_error_name_is_blank),
                "Blank device name"
            ),
            arrayOf(
                MY_DEVICE_1,
                getRandomString(256),
                StringUtils.stringFromResource(I18N.string.common_error_name_too_long, 255),
                "Too long (256) name"
            )
        )
    }
}
