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

package me.proton.android.drive.ui.viewstate

import androidx.compose.runtime.Immutable

@Immutable
data class PhotosBackupViewState(
    val title: String,
    val backup: PhotosBackupOption,
    val mobileData: PhotosBackupOption,
    val ignoringBatteryOptimizations: PhotosBackupOption,
    val tagsMigrationProgressState: TagsMigrationProgressState? = null,
)

@Immutable
data class PhotosBackupOption(
    val title: String,
    val checked: Boolean,
    val enabled: Boolean = true,
    val description: String? = null,
)

@Immutable
data class TagsMigrationProgressState(
    val title: String,
    val description: String,
    val progress: Float? = null,
)
