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
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.files.presentation.component.FilesTestTag
import me.proton.core.drive.files.presentation.component.files.FilesListItemComponentTestTag
import me.proton.core.drive.files.presentation.component.files.FilesListItemComponentTestTag.ItemType
import me.proton.core.test.android.instrumented.utils.StringUtils

object FilesTabRobot : HomeRobot {
    private val uploadFileDesc =
        StringUtils.stringFromResource(R.string.files_upload_content_description_upload_file)
    private val plusButton get() = node(hasContentDescription(uploadFileDesc))
    private val filesListItems get() = nodes(hasTestTag(FilesListItemComponentTestTag.item))

    private fun moreButton(itemName: String, itemType: ItemType) =
        node(
            hasTestTag(FilesListItemComponentTestTag.threeDotsButton(itemType)),
            hasAnySibling(hasTextExactly(itemName))
        )

    private val filesContent get() = node(hasTestTag(FilesTestTag.content))

    fun itemWithTextDisplayed(text: String) {
        filesListItems.filter(hasText(text)).assertCountEquals(1)
    }

    fun swipeUpToItemWithName(itemName: String): FilesTabRobot = waitFor(this) {
        try {
            node(hasText(itemName)).assertIsDisplayed()
        } catch (error: AssertionError) {
            filesContent.performTouchInput { swipeUp(durationMillis = 1000L) }
            throw error
        }
    }

    fun clickPlusButton() = plusButton.tryToClickAndGoTo(ParentFolderOptionsRobot)

    fun clickMoreOnItem(title: String) =
        node(
            hasTestTag(FilesListItemComponentTestTag.threeDotsButton(ItemType.File)) or hasTestTag(FilesListItemComponentTestTag.threeDotsButton(ItemType.Folder)),
            hasAnySibling(hasTextExactly(title))
        ).tryToClickAndGoTo(FileFolderOptionsRobot)

    fun clickMoreOnFolder(title: String) =
        moreButton(title, ItemType.Folder).tryToClickAndGoTo(FileFolderOptionsRobot)

    fun clickMoreOnFile(title: String) =
        moreButton(title, ItemType.File).tryToClickAndGoTo(FileFolderOptionsRobot)

    fun clickOnFile(name: String) =
        node(
            hasTestTag(FilesListItemComponentTestTag.threeDotsButton(ItemType.File)),
            hasAnyChild(hasText(name))
        ).tryToClickAndGoTo(PreviewRobot)

    fun clickOnFolder(name: String) =
        node(
            hasAnySibling(hasTestTag(FilesListItemComponentTestTag.threeDotsButton(ItemType.Folder))),
            hasText(name)
        ).tryToClickAndGoTo(FilesTabRobot)

    override fun robotDisplayed() {
        homeScreenDisplayed()
        filesTab.assertIsSelected()
    }
}