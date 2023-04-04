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
import me.proton.android.drive.R
import me.proton.android.drive.ui.screen.MoveToFolderScreenTestTag
import me.proton.core.drive.files.presentation.component.FilesTestTag
import me.proton.core.drive.files.presentation.component.files.FilesListItemComponentTestTag

object MoveToFolderRobot : Robot {
    private val moveToFolderScreen get() = node.withTag(MoveToFolderScreenTestTag.screen)
    private val addFolderButton get() = node.withTag(MoveToFolderScreenTestTag.plusFolderButton)
    private val cancelButton get() = node.withText(R.string.move_file_dismiss_action)
    private val moveButton get() = node.withText(R.string.move_file_confirm_action)
    private val fileList get() = node.withTag(FilesTestTag.content)
    private fun itemWithName(name: String) =
        node.withTag(FilesListItemComponentTestTag.item).withText(name)

    fun clickAddFolder() = addFolderButton.clickTo(CreateFolderRobot)
    fun clickCancel() = cancelButton.clickTo(CreateFolderRobot)
    fun clickMove() = moveButton.clickTo(CreateFolderRobot)

    fun itemWithTextDisplayed(text: String) {
        fileList.scrollTo(node.withText(text))
    }

    override fun robotDisplayed() {
        moveToFolderScreen.assertIsDisplayed()
        addFolderButton.assertIsDisplayed()
        cancelButton.assertIsDisplayed()
        moveButton.assertIsDisplayed()
    }
}
