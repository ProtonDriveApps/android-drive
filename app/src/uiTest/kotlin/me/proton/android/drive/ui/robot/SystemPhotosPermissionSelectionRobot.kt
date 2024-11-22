/*
 * Copyright (c) 2024 Proton AG.
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

import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import me.proton.android.drive.test.R
import me.proton.test.fusion.Fusion.byObject
import me.proton.test.fusion.Fusion.byObjects

interface SystemPhotosPermissionSelectionRobot : Robot {

    fun select() = this.apply {
        byObjects
            .withContentDescStartsWith(
                getInstrumentation().context.resources.getString(R.string.test_photos_permissions_selected_photo_content_description)
            )
            .isClickable()
            .isEnabled()
            .atPosition(0)
            .click()
    }

    fun <T : Robot> clickAllow(goesTo: T): T = goesTo.apply {
        byObject
            .withResName("com.google.android.providers.media.module:id/button_add")
            .waitForClickable()
            .click()
    }
}
