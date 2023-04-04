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


import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.files.presentation.component.FilesTestTag
import me.proton.core.drive.files.presentation.component.files.FilesListItemComponentTestTag.threeDotsButton
import me.proton.core.drive.files.presentation.component.files.FilesListItemComponentTestTag.item
import me.proton.core.drive.files.presentation.component.files.FilesListItemComponentTestTag.ItemType
import me.proton.test.fusion.Fusion.allNodes
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.ui.common.enums.SwipeDirection

object FilesTabRobot : HomeRobot {
    private val plusButton get() = node.withContentDescription(R.string.files_upload_content_description_upload_file)
    private fun itemWithName(name: String) = node.withTag(item).withText(name)
    private val fileList get() = node.withTag(FilesTestTag.content)
    private fun moreButton(itemName: String, itemType: ItemType) =
        node
            .withTag(threeDotsButton(itemType))
            .hasSibling(node.withText(itemName))

    fun itemWithTextDisplayed(text: String) {
        itemWithName(text).await { assertIsDisplayed() }
    }

    fun scrollToItemWithName(itemName: String): FilesTabRobot = apply {
        allNodes.withTag(item).assertAny(node.isEnabled())
        fileList.scrollTo(node.withText(itemName))
    }

    fun clickPlusButton() = plusButton.clickTo(ParentFolderOptionsRobot)

    fun clickMoreOnItem(title: String) =
        node
            .withAnyTag(threeDotsButton(ItemType.File), threeDotsButton(ItemType.Folder))
            .hasSibling(node.withText(title))
            .clickTo(FileFolderOptionsRobot)

    fun clickMoreOnFolder(title: String) =
        moreButton(title, ItemType.Folder).clickTo(FileFolderOptionsRobot)

    fun clickMoreOnFile(title: String) =
        moreButton(title, ItemType.File).clickTo(FileFolderOptionsRobot)

    fun clickOnFile(name: String) =
        node
            .withTag(threeDotsButton(ItemType.File))
            .hasChild(node.withText(name))
            .clickTo(this)

    fun clickOnFolder(name: String) =
        node
            .withText(name)
            .hasSibling(node.withTag(threeDotsButton(ItemType.Folder)))
            .clickTo(this)

    override fun robotDisplayed() {
        homeScreenDisplayed()
        filesTab.assertIsSelected()
    }
}