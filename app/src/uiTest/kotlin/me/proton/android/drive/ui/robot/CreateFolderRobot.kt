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

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import me.proton.core.drive.files.presentation.component.files.FilesListItemComponentTestTag
import me.proton.core.drive.folder.create.presentation.CreateFolderComponentTestTag
import me.proton.core.drive.base.presentation.R as BasePresentation

object CreateFolderRobot : Robot {
    private val createFolderScreen get() = node(hasTestTag(CreateFolderComponentTestTag.screen))
    private val cancelButton get() = node(hasTextResource(BasePresentation.string.common_cancel_action))
    private val createButton get() = node(hasTextResource(BasePresentation.string.common_create_action))
    private val folderNameField
        get() = node(
            hasSetTextAction(),
            hasAnyAncestor(hasTestTag(CreateFolderComponentTestTag.folderNameTextField))
        )

    fun clickCancel() = cancelButton.tryToClickAndGoTo(FilesTabRobot)
    fun typeFolderName(text: String) = folderNameField.tryToTypeText(text, CreateFolderRobot)
    fun clearName() = folderNameField.clearText(CreateFolderRobot)

    fun clickCreate() {
        createButton.tryToClickAndGoTo(this)
    }

    override fun robotDisplayed() {
        createFolderScreen.assertIsDisplayed()
    }
}
