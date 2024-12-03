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
import me.proton.android.drive.ui.robot.ComputersTabRobot
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

@HiltAndroidTest
class RenamingComputerSuccessFlowTest : BaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isDevice = true)
    fun renameComputer() {
        val computerToBeRenamed = MY_DEVICE_1
        val newComputerName = "My device X"
        FilesTabRobot
            .clickComputersTab()
            .scrollToComputer(computerToBeRenamed)
            .clickMoreOnComputer(computerToBeRenamed)
            .clickRename()
            .clearName()
            .typeName(newComputerName)
            .clickRename(ComputersTabRobot)
            .verify {
                itemIsDisplayed(newComputerName)
            }
    }

    companion object {
        private const val MY_DEVICE_1 = "MyDevice1"
    }
}
