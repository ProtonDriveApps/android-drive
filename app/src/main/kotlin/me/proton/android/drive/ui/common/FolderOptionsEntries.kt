/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.files.presentation.entry.FileOptionEntry

inline fun folderEntry(
    @DrawableRes icon: Int,
    @StringRes labelResId: Int,
    notificationDotVisible: Boolean = false,
    crossinline runAction: (suspend () -> Unit) -> Unit,
    crossinline block: DriveLink.Folder.() -> Unit,
) = FolderEntry(
    icon = icon,
    labelResId = labelResId,
    notificationDotVisible = notificationDotVisible,
    onClick = { driveLink -> runAction { driveLink.block() } }
)

class FolderEntry(
    @DrawableRes override val icon: Int,
    @StringRes private val labelResId: Int,
    override val notificationDotVisible: Boolean,
    override val onClick: (DriveLink.Folder) -> Unit,
) : FileOptionEntry.SimpleEntry<DriveLink.Folder> {

    @Composable
    override fun getLabel(): String = stringResource(id = labelResId)
}
