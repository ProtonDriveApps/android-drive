/*
 * Copyright (c) 2025 Proton AG.
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

import me.proton.android.drive.ui.dialog.ConfirmDeleteAlbumDialogTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object ConfirmDeleteAlbumRobot : Robot {
    private val withChildrenDialog = node.withTag(
        ConfirmDeleteAlbumDialogTestTag.withChildrenDialog
    )
    private val withoutChildrenDialog = node.withTag(
        ConfirmDeleteAlbumDialogTestTag.withoutChildrenDialog
    )
    private val cancelButton = node.withText(I18N.string.common_cancel_action)
    private val deleteAlbumButton = node.withText(
        I18N.string.albums_delete_album_dialog_delete_album_action
    )
    private val deleteWithoutSavingButton = node.withText(
        I18N.string.albums_delete_album_dialog_delete_without_saving_action
    )
    private val saveAndDeleteButton = node.withText(
        I18N.string.albums_delete_album_dialog_save_and_delete_action
    )

    fun <T : Robot> clickOnCancel(goesTo: T) = cancelButton.clickTo(goesTo)
    fun <T : Robot> clickOnDeleteAlbum(goesTo: T) = deleteAlbumButton.clickTo(goesTo)
    fun <T : Robot> clickOnDeleteWithoutSaving(goesTo: T) = deleteWithoutSavingButton.clickTo(goesTo)
    fun <T : Robot> clickOnSaveAndDelete(goesTo: T) = saveAndDeleteButton.clickTo(goesTo)

    fun assertWithChildrenDialogIsDisplayed() = withChildrenDialog.await { assertIsDisplayed() }
    fun assertWithoutChildrenDialogIsDisplayed() = withoutChildrenDialog.await { assertIsDisplayed() }

    override fun robotDisplayed() {
        error("Use dedicated assert functions")
    }
}
