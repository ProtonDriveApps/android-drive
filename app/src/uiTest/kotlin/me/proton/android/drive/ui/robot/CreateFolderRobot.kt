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

package me.proton.android.drive.ui.robot

import me.proton.core.drive.folder.create.presentation.CreateFolderComponentTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object CreateFolderRobot : Robot {
    private val createFolderScreen get() = node.withTag(CreateFolderComponentTestTag.screen)
    private val cancelButton get() = node.withText(I18N.string.common_cancel_action)
    private val createButton get() = node.withText(I18N.string.common_create_action)
    private val folderNameField get() = node.isSetText().hasAncestor(
        node.withTag(CreateFolderComponentTestTag.folderNameTextField)
    )

    fun clickCancel() = cancelButton.clickTo(FilesTabRobot)
    fun typeFolderName(text: String) = apply { folderNameField.typeText(text) }
    fun clearName() = apply { folderNameField.clearText() }
    fun <T : Robot> clickCreate(goesTo: T) : T = createButton.clickTo(goesTo)

    override fun robotDisplayed() {
        createFolderScreen.assertIsDisplayed()
    }
}
