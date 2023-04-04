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

import me.proton.test.fusion.Fusion.node
import me.proton.android.drive.ui.dialog.FileFolderOptionsDialogTestTag
import me.proton.core.drive.base.presentation.R


object FileFolderOptionsRobot : Robot {
    private val fileFolderOptionsScreen get() = node.withTag(FileFolderOptionsDialogTestTag.fileOrFolderOptions)
    private val moveButton get() = node.withText(R.string.files_move_file_action)
    private val moveToTrash get() = node.withText(R.string.files_send_to_trash_action)
    private val makeAvailableOfflineButton get() = node.withText(R.string.title_make_available_offline)
    private val getLinkButton get() = node.withText(R.string.title_get_link)
    private val renameButton get() = node.withText(R.string.files_rename_file_action)
    private val folderDetailsButton get() = node.withText(R.string.files_display_folder_info_action)

    fun clickMove() = moveButton.clickTo(MoveToFolderRobot)
    fun clickRename() = renameButton.clickTo(RenameRobot)

    override fun robotDisplayed() {
        fileFolderOptionsScreen.assertIsDisplayed()
    }
}
