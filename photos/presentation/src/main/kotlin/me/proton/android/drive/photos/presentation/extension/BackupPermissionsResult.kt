/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.android.drive.photos.presentation.extension

import android.Manifest
import android.os.Build

/**
 * Whether the result of a backup permissions request allows backup: either every permission was
 * granted, or access to selected photos only, which denies the other media permissions.
 */
fun Map<String, Boolean>.allowsBackup(): Boolean = values.all { granted -> granted } || hasPartialPermissions()

private fun Map<String, Boolean>.hasPartialPermissions() =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        this[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
