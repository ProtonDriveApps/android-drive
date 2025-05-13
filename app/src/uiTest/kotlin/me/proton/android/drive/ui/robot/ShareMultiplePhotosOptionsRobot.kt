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

import me.proton.android.drive.ui.dialog.ShareMultiplePhotosOptionsTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object ShareMultiplePhotosOptionsRobot : Robot {
    private val shareMultiplePhotosOptionsScreen get() = node.withTag(ShareMultiplePhotosOptionsTestTag.screen)
    private val newSharedAlbumOption get() = node.withText(I18N.string.albums_share_multiple_photos_options_new_shared_album)

    fun clickOnNewSharedAlbum() = CreateAlbumTabRobot.apply {
        newSharedAlbumOption.scrollTo().click()
    }

    fun clickOnSharedAlbum(name: String) = AlbumRobot.apply {
        node.withText(name).click()
    }

    override fun robotDisplayed() {
        shareMultiplePhotosOptionsScreen.await { assertIsDisplayed() }
    }
}
