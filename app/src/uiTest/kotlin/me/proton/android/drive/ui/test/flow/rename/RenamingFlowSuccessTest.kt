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

package me.proton.android.drive.ui.test.flow.rename

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@HiltAndroidTest
@RunWith(Parameterized::class)
class RenamingFlowSuccessTest(
    private val itemToBeRenamed: String,
    private val newItemName: String
): BaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 4)
    fun renameSuccess() {
        PhotosTabRobot
            .clickFilesTab()
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
