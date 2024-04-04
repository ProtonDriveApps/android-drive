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
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.android.drive.utils.getRandomString
import me.proton.core.test.android.instrumented.utils.StringUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
@RunWith(Parameterized::class)
class RenamingFolderFlowErrorTest(
    private val itemToBeRenamed: String,
    private val newItemName: String,
    private val errorMessage: String,
    @Suppress("unused") private val friendlyName: String
) : AuthenticatedBaseTest() {

    @Test
    @Scenario(4)
    fun renameError() {
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(itemToBeRenamed)
            .clickMoreOnItem(itemToBeRenamed)
            .clickRename()
            .clearName()
            .typeName(newItemName)
            .clickRename()
            .verify {
                nodeWithTextDisplayed(errorMessage)
            }
    }

    companion object {
        @get:Parameterized.Parameters(name = "{3}")
        @get:JvmStatic
        val data = listOf(
            arrayOf(
                "folder1",
                "folder2",
                "A file or folder with that name already exists",
                "Existing folder"
            ),
            arrayOf(
                "folder1",
                "",
                StringUtils.stringFromResource(I18N.string.common_error_name_is_blank),
                "Empty folder name"
            ),
            arrayOf(
                "folder1",
                getRandomString(256),
                StringUtils.stringFromResource(I18N.string.common_error_name_too_long, 255),
                "Very long name"
            ),
            arrayOf(
                "folder1",
                "folder1/",
                StringUtils.stringFromResource(I18N.string.common_error_name_with_forbidden_characters),
                "Forbidden characters"
            ),
        )
    }
}
