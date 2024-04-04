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

package me.proton.android.drive.photos.presentation.viewevent

import android.content.Context
import androidx.paging.CombinedLoadStates
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.LinkId

interface PhotosViewEvent {
    val onScroll: (Int, Set<LinkId>) -> Unit get() = { _, _ -> }
    val onStatusClicked: () -> Unit get() = {}
    val onTopAppBarNavigation: () -> Unit get() = {}
    val onDriveLink: (DriveLink) -> Unit get() = {}

    val onLoadState: (CombinedLoadStates, Int) -> Unit
    val onRefresh: () -> Unit get() = {}
    val onErrorAction: () -> Unit get() = {}
    val onSelectedOptions: () -> Unit get() = {}
    val onSelectDriveLink: (DriveLink) -> Unit get() = {}
    val onDeselectDriveLink: (DriveLink) -> Unit get() = {}
    val onBack: () -> Unit get() = {}

    val onPermissionsChanged: (BackupPermissions) -> Unit get() = {}
    val onEnable: () -> Unit get() = {}
    val onPermissions: () -> Unit get() = {}
    val onRetry: () -> Unit get() = {}
    val onGetStorage: () -> Unit get() = {}
    val onResolveMissingFolder: () -> Unit get() = {}
    val onChangeNetwork: () -> Unit get() = {}
    val onIgnoreBackgroundRestrictions: (Context) -> Unit get() = {}
    val onDismissBackgroundRestrictions: () -> Unit get() = {}
    val onResolve: () -> Unit get() = {}
    val onShowUpsell: () -> Unit
}
