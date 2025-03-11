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

import me.proton.android.drive.ui.screen.AlbumScreenTestTag
import me.proton.core.drive.i18n.R as I18N
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.FusionConfig.targetContext

object AlbumRobot : LinksRobot, NavigationBarRobot {
    private val albumScreen get() = node.withTag(AlbumScreenTestTag.screen)

    fun assertAlbumNameIsDisplayed(name: String) = node.withText(name)
        .await { assertIsDisplayed() }

    fun assertItemsInAlbum(count: Int) = node.withTextSubstring(
        targetContext.resources.getQuantityString(
            I18N.plurals.albums_photo_count,
            count
        ).format(count)
    ).await { assertIsDisplayed() }

    override fun robotDisplayed() {
        albumScreen.await { assertIsDisplayed() }
    }
}
