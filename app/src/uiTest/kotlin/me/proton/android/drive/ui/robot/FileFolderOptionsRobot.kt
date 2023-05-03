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

import me.proton.android.drive.ui.dialog.FileFolderOptionsDialogTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N


object FileFolderOptionsRobot : Robot {
    private val fileFolderOptionsScreen get() = node.withTag(FileFolderOptionsDialogTestTag.fileOrFolderOptions)
    private val moveButton get() = node.withText(I18N.string.files_move_file_action)
    private val moveToTrash get() = node.withText(I18N.string.files_send_to_trash_action)
    private val makeAvailableOfflineButton get() = node.withText(I18N.string.common_make_available_offline_action)
    private val getLinkButton get() = node.withText(I18N.string.common_get_link_action)
    private val manageLinkButton get() = node.withText(I18N.string.common_manage_link_action)
    private val renameButton get() = node.withText(I18N.string.files_rename_file_action)
    private val folderDetailsButton get() = node.withText(I18N.string.files_display_folder_info_action)
    private val stopSharingButton get() = node.withText(I18N.string.common_stop_sharing_action)

    fun clickMove() = moveButton.clickTo(MoveToFolderRobot)
    fun clickRename() = renameButton.clickTo(RenameRobot)
    fun clickGetLink() = getLinkButton.clickTo(ShareRobot)
    fun clickManageLink() = manageLinkButton.clickTo(ShareRobot)
    fun clickStopSharing() = stopSharingButton.clickTo(ConfirmStopSharingRobot)

    override fun robotDisplayed() {
        fileFolderOptionsScreen.assertIsDisplayed()
    }
}
