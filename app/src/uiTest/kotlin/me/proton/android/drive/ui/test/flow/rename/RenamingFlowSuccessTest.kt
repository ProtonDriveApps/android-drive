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

package me.proton.android.drive.ui.test.flow.rename

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@HiltAndroidTest
@RunWith(Parameterized::class)
class RenamingFlowSuccessTest(
    private val itemToBeRenamed: String,
    private val newItemName: String
): AuthenticatedBaseTest() {

    @Test
    @Scenario(4)
    fun renameSuccess() {
        FilesTabRobot
            .scrollToItemWithName(itemToBeRenamed)
            .clickMoreOnItem(itemToBeRenamed)
            .clickRename()
            .clearName()
            .typeName(newItemName)
            .clickRename()
            .scrollToItemWithName(newItemName)
            .verify {
                itemIsDisplayed(newItemName)
            }
    }

    companion object {
        @get:Parameterized.Parameters(name = "folderToBeRenamed={0}_newFolderName={1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf("folder3", "FoLdEr3"),
            arrayOf("conf.json", "conf"),
        )
    }
}
