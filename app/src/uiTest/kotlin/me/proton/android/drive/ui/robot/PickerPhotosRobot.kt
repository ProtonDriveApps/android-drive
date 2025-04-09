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

import me.proton.android.drive.ui.screen.PickerPhotosAndAlbumsScreenTestTag
import me.proton.core.drive.i18n.R
import me.proton.test.fusion.Fusion.node

interface PickerPhotosRobot : Robot {

    private val resetButton get() = node.withTag(PickerPhotosAndAlbumsScreenTestTag.resetButton)
    private val addToAlbumButton get() = node.withTag(PickerPhotosAndAlbumsScreenTestTag.addToAlbumButton)

    fun <T : Robot> clickOnReset(goesTo: T) = resetButton.clickTo(goesTo)
    fun <T : Robot> clickOnAddToAlbum(goesTo: T) = addToAlbumButton.clickTo(goesTo)

    fun assertTotalPhotosToAddToAlbum(count: Int) =
        if (count == 0) {
            node.withText(R.string.albums_add_zero_to_album_button)
        } else {
            node.withPluralTextResource(R.plurals.albums_add_non_zero_to_album_button, count)
        }.await { assertIsDisplayed() }
}
