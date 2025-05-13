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

import me.proton.android.drive.ui.dialog.ConfirmLeaveAlbumDialogTestTag
import me.proton.core.test.android.instrumented.FusionConfig
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object ConfirmLeaveAlbumRobot : Robot {
    private val dialog get() = node.withTag(ConfirmLeaveAlbumDialogTestTag.confirmLeaveAlbum)

    private val cancelButton = node.withText(I18N.string.common_cancel_action)
    private val leaveWithoutSavingButton = node.withText(I18N.string.albums_leave_album_dialog_leave_without_saving_action)
    private val saveAndLeaveButton = node.withText(I18N.string.albums_leave_album_dialog_save_and_leave_action)

    fun <T : Robot> clickOnCancel(goesTo: T) = cancelButton.clickTo(goesTo)
    fun <T : Robot> clickOnLeaveWithoutSaving(goesTo: T) = leaveWithoutSavingButton.clickTo(goesTo)
    fun <T : Robot> clickOnSaveAndLeave(goesTo: T) = saveAndLeaveButton.clickTo(goesTo)

    fun assertDescriptionIsDisplayed(album: String) =
        node.withText(
            FusionConfig.targetContext().getString(I18N.string.albums_leave_album_dialog_description, album)
        ).await { assertIsDisplayed() }

    override fun robotDisplayed() {
        dialog.await { assertIsDisplayed() }
    }
}
