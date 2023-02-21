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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import me.proton.android.drive.R
import me.proton.android.drive.ui.screen.MoveToFolderScreenTestTag
import me.proton.core.drive.files.presentation.component.FilesTestTag
import me.proton.core.drive.files.presentation.component.files.FilesListItemComponentTestTag
import me.proton.core.test.android.instrumented.utils.StringUtils

object MoveToFolderRobot : Robot {
    private val moveToFolderScreen get() = node(hasTestTag(MoveToFolderScreenTestTag.screen))
    private val addFolderButton get() = node(hasTestTag(MoveToFolderScreenTestTag.plusFolderButton))
    private val cancelButton get() = node(hasText(StringUtils.stringFromResource(R.string.move_file_dismiss_action)))
    private val moveButton get() = node(hasText(StringUtils.stringFromResource(R.string.move_file_confirm_action)))
    private val filesListItems get() = nodes(hasTestTag(FilesListItemComponentTestTag.item))
    private val filesContent get() = node(hasTestTag(FilesTestTag.content))

    fun clickAddFolder() = addFolderButton.tryToClickAndGoTo(CreateFolderRobot)
    fun clickCancel() = cancelButton.tryToClickAndGoTo(CreateFolderRobot)
    fun clickMove() = moveButton.tryToClickAndGoTo(CreateFolderRobot)

    fun swipeDown() = filesContent.tryPerformTouchInputAndGoTo(this) { swipeDown()}

    fun itemListWithTextDisplayed(text: String, count: Int = 1) {
        filesListItems.filter(hasText(text)).assertCountEquals(count)
    }

    override fun robotDisplayed() {
        moveToFolderScreen.assertIsDisplayed()
    }
}
