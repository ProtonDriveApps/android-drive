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

import me.proton.android.drive.ui.extension.withItemType
import me.proton.android.drive.ui.extension.withLayoutType
import me.proton.android.drive.ui.extension.withLinkName
import me.proton.android.drive.ui.screen.AlbumScreenTestTag
import me.proton.core.drive.files.presentation.extension.ItemType
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.FusionConfig.targetContext
import me.proton.core.drive.i18n.R as I18N

object AlbumRobot : LinksRobot, NavigationBarRobot {
    private val albumScreen get() = node.withTag(AlbumScreenTestTag.screen)
    private val moreButton get() = node.withContentDescription(I18N.string.common_more)
    private val addButton get() = node.withText(I18N.string.common_add_action)

    fun clickOnMoreButton() = moreButton.clickTo(AlbumOptionsRobot)

    fun clickOnAdd() = addButton.clickTo(PickerPhotosAndAlbumsRobot)

    fun clickOnPhoto(name: String) =
        photoWithName(name).clickTo(PreviewRobot)

    private fun photoWithName(name: String) = linkWithName(name)
        .withItemType(ItemType.File)
        .withLayoutType(LayoutType.Grid)

    fun assertAlbumNameIsDisplayed(name: String) = node.withText(name)
        .await { assertIsDisplayed() }

    fun assertItemsInAlbum(count: Int) = node.withTextSubstring(
        targetContext.resources.getQuantityString(
            I18N.plurals.albums_photo_count,
            count
        ).format(count)
    ).await { assertIsDisplayed() }

    fun assertCoverAlbum(name: String) = node.withLinkName(name)
        .withItemType(ItemType.File)
        .withLayoutType(LayoutType.Cover)
        .await { assertIsDisplayed() }

    override fun robotDisplayed() {
        albumScreen.await { assertIsDisplayed() }
    }
}
