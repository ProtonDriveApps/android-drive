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

import android.os.Build
import me.proton.test.fusion.Fusion.byObject

interface SystemNotificationPermissionRobot : Robot {

    fun <T : Robot> allowPermission(goesTo: T): T = goesTo.apply {
        require(Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            "This should not be called on API ${Build.VERSION_CODES.S_V2} or lower"
        }
        byObject.withResName(
            "com.android.permissioncontroller:id/permission_allow_button"
        ).waitForClickable().click()
    }

    fun <T : Robot> denyPermission(goesTo: T): T = goesTo.apply {
        require(Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            "This should not be called on API ${Build.VERSION_CODES.S_V2} or lower"
        }
        byObject.withResName(
            "com.android.permissioncontroller:id/permission_deny_button"
        ).waitForClickable().click()
    }
}
