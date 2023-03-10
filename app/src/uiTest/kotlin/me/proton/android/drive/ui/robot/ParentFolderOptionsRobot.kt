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

package me.proton.android.drive.ui.robot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import me.proton.android.drive.R
import me.proton.android.drive.ui.dialog.ParentFolderOptionsDialogTestTag

object ParentFolderOptionsRobot : Robot {
    private val contextMenu get() = node(hasTestTag(ParentFolderOptionsDialogTestTag.contextMenu))
    private val createFolderButton get() = node(hasTextResource(R.string.folder_option_create_folder))
    private val uploadAFileButton get() = node(hasTextResource(R.string.folder_option_import_file))
    private val takePhotoButton get() = node(hasTextResource(R.string.folder_option_take_a_photo))

    fun clickCreateFolder() = createFolderButton.tryToClickAndGoTo(CreateFolderRobot)

    override fun robotDisplayed() {
        contextMenu.assertIsDisplayed()
    }
}
