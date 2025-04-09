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
import me.proton.android.drive.ui.screen.PickerPhotosAndAlbumsScreenTestTag
import me.proton.core.drive.files.presentation.extension.ItemType
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object PickerPhotosAndAlbumsRobot : PickerPhotosRobot, LinksRobot, NavigationBarRobot {
    private val screen get() = node.withTag(PickerPhotosAndAlbumsScreenTestTag.screen)
    private val photosTab get() = node.withText(I18N.string.photos_title)
    private val albumsTab get() = node.withText(I18N.string.albums_title)

    fun clickOnPhotosTab() = photosTab.clickTo(PickerPhotosAndAlbumsRobot)
    fun clickOnAlbumsTab() = albumsTab.clickTo(PickerPhotosAndAlbumsRobot)
    fun clickOnPhoto(name: String) = photoWithName(name).clickTo(PickerPhotosAndAlbumsRobot)

    private fun photoWithName(name: String) = linkWithName(name)
        .withItemType(ItemType.File)
        .withLayoutType(LayoutType.Grid)

    override fun robotDisplayed() {
        screen.await { assertIsDisplayed() }
    }
}
