/*
 * Copyright (c) 2023-2024 Proton AG.
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

import me.proton.android.drive.photos.presentation.component.CreateNewAlbumTestTag
import me.proton.test.fusion.Fusion
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object CreateAlbumTabRobot : Robot {
    private val newAlbumHint
        get() = node
            .isSetText()
            .hasDescendant(node.withText(I18N.string.albums_new_album_name_hint))

    private val doneButton get() = node.withText(I18N.string.common_done_action)
    private val addButton get() = node.withText(I18N.string.common_add_action)

    fun typeName(text: String) = apply { newAlbumHint.typeText(text) }

    fun clickOnRemoveFirstPhoto() =
        Fusion.allNodes
            .withTag(CreateNewAlbumTestTag.removePhotoButton)
            .onFirst()
            .clickTo(CreateAlbumTabRobot)

    fun <T : Robot> clickOnDone(goesTo: T) = doneButton.clickTo(goesTo)

    fun clickOnAdd() = addButton.clickTo(PickerPhotosAndAlbumsRobot)

    override fun robotDisplayed() {
        newAlbumHint.assertIsDisplayed()
    }
}
