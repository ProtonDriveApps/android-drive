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

import me.proton.core.drive.drivelink.rename.presentation.RenameScreenTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object RenameRobot : Robot {
    private val renameScreen get() = node.withTag(RenameScreenTestTag.screen)
    private val cancelRenameButton get() = node.withText(I18N.string.link_rename_dismiss_button)
    private val confirmRenameButton get() = node.withText(I18N.string.link_rename_button)
    private val renameTextField get() = node.isSetText().hasAncestor(node.withTag(RenameScreenTestTag.textField))

    fun clickCancel() = cancelRenameButton.clickTo(FilesTabRobot)
    fun clickRename() = confirmRenameButton.clickTo(FilesTabRobot)
    fun <T: Robot> clickRename(goesTo: T) = confirmRenameButton.clickTo(goesTo)
    fun typeName(text: String) = apply { renameTextField.typeText(text) }
    fun clearName() = apply { renameTextField.clearText() }

    override fun robotDisplayed() {
        renameScreen.await { assertIsDisplayed() }
    }
}
