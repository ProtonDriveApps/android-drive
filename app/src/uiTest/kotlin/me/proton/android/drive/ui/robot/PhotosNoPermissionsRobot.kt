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

import me.proton.android.drive.ui.extension.withTextResource
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N


object PhotosNoPermissionsRobot : SystemPhotosNoPermissionRobot, Robot {
    private val photoPermissionsTitle =
        node.withTextResource(I18N.string.photos_permission_rational_title, I18N.string.app_name)
    private val photosPermissionDescription =
        node.withTextResource(I18N.string.photos_permission_rational_description, I18N.string.app_name)
    private val settingsButton = node.withText(I18N.string.photos_permission_rational_confirm_action)
    private val notNowButton = node.withText(I18N.string.photos_permission_rational_dismiss_action)
    private val allowAccessImage = node.withText(I18N.string.photos_permission_rational_img_text)

    fun <T: Robot> clickNotNow(goesTo: T) = notNowButton.clickTo(goesTo)

    override fun robotDisplayed() {
        photoPermissionsTitle.await { assertIsDisplayed() }
        photosPermissionDescription.await { assertIsDisplayed() }
        notNowButton.await { assertIsDisplayed() }
        settingsButton.await { assertIsDisplayed() }
        allowAccessImage.await { assertIsDisplayed() }
    }
}
