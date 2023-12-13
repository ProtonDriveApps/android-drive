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

import me.proton.android.drive.ui.extension.assertDownloadState
import me.proton.android.drive.ui.extension.assertHasItemType
import me.proton.android.drive.ui.extension.assertHasLayoutType
import me.proton.android.drive.ui.extension.assertHasThumbnail
import me.proton.android.drive.ui.extension.doesNotExist
import me.proton.android.drive.ui.extension.withItemType
import me.proton.android.drive.ui.extension.withLayoutType
import me.proton.android.drive.ui.extension.withLinkName
import me.proton.core.drive.files.presentation.component.FilesTestTag
import me.proton.core.drive.files.presentation.component.files.FilesListHeaderTestTag
import me.proton.core.drive.files.presentation.extension.ItemType
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.core.drive.files.presentation.extension.SemanticsDownloadState
import me.proton.core.drive.i18n.R
import me.proton.test.fusion.Fusion.node

@Suppress("TooManyFunctions")
interface LinksRobot : Robot {

    private val content get() = node.withTag(FilesTestTag.content)

    private val layoutSwitcher get() = node.withTag(FilesListHeaderTestTag.layoutSwitcher)

    private val selectedOptionsButton get() = node
        .withContentDescription(R.string.content_description_selected_options)

    fun linkWithName(name: String) = node.withLinkName(name).isClickable()

    private fun moreButton(name: String) =
        node.withTag(FilesTestTag.moreButton).hasAncestor(linkWithName(name))

    fun clickLayoutSwitcher() = layoutSwitcher.clickTo(FilesTabRobot)

    fun scrollToItemWithName(itemName: String): LinksRobot = apply {
        content.scrollTo(linkWithName(itemName))
    }

    fun clickOptions() =
        selectedOptionsButton
            .clickTo(FileFolderOptionsRobot)

    fun <T : Robot> clickOnItem(
        name: String,
        layoutType: LayoutType,
        itemType: ItemType,
        goesTo: T
    ) =
        linkWithName(name)
            .withLayoutType(layoutType)
            .withItemType(itemType)
            .clickTo(goesTo)

    fun clickOnFolder(name: String, layoutType: LayoutType = LayoutType.List): FilesTabRobot =
        clickOnItem(name, layoutType, ItemType.Folder, FilesTabRobot)

    fun clickOnFolderToMove(name: String) =
        clickOnItem(name, LayoutType.List, ItemType.Folder, MoveToFolderRobot)

    fun clickMoreOnItem(name: String) =
        moreButton(name).clickTo(FileFolderOptionsRobot)

    fun clickOnFile(name: String, layoutType: LayoutType = LayoutType.List) =
        clickOnItem(name, layoutType, ItemType.File, PreviewRobot)

    fun longClickOnItem(name: String) =
        linkWithName(name).longClickTo(FilesTabRobot)

    fun itemIsDisplayed(
        name: String,
        layoutType: LayoutType? = null,
        itemType: ItemType? = null,
        hasThumbnail: Boolean? = null,
        downloadState: SemanticsDownloadState? = null
    ) {
        linkWithName(name)
            .await {
                layoutType?.let { assertHasLayoutType(it) }
                itemType?.let { assertHasItemType(it) }
                hasThumbnail?.let { assertHasThumbnail(it) }
                downloadState?.let { assertDownloadState(it) }
                assertIsDisplayed()
            }
    }

    fun itemIsNotDisplayed(name: String) {
        linkWithName(name)
            .await {
                doesNotExist()
            }
    }
}
