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
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import me.proton.core.drive.drivelink.rename.presentation.R
import me.proton.core.drive.drivelink.rename.presentation.RenameScreenTestTag

object RenameRobot : Robot {

    private val renameScreen get() = node(hasTestTag(RenameScreenTestTag.screen))
    private val cancelRenameButton get() = node(hasTextResource(R.string.link_rename_dismiss_button))
    private val confirmRenameButton get() = node(hasTextResource(R.string.link_rename_button))
    private val renameTextField
        get() = node(
            hasSetTextAction(),
            hasAnyAncestor(hasTestTag(RenameScreenTestTag.textField))
        )

    fun clickCancel() = cancelRenameButton.tryToClickAndGoTo(FilesTabRobot)
    fun clickRename() = confirmRenameButton.tryToClickAndGoTo(FilesTabRobot)
    fun typeName(text: String) = renameTextField.tryToTypeText(text, RenameRobot)
    fun clearName() = renameTextField.clearText(RenameRobot)

    override fun robotDisplayed() {
        renameScreen.assertIsDisplayed()
    }
}
