/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.user.presentation.user.extension

import me.proton.core.drive.i18n.R
import me.proton.core.user.domain.entity.User

/** Returns data for displaying current storage status for the user.
 * @return Triple of <used_drive_space_bytes, max_drive_space_bytes, string_res_id>.
 */
fun User.getStorageIndicatorData(): Triple<Long, Long, Int> {
    val usedDriveSpace = usedDriveSpace
    val maxDriveSpace = maxDriveSpace
    return if (usedDriveSpace != null && maxDriveSpace != null) {
        Triple(usedDriveSpace, maxDriveSpace, R.string.storage_drive_usage)
    } else {
        Triple(usedSpace, maxSpace, R.string.storage_total_usage)
    }
}
