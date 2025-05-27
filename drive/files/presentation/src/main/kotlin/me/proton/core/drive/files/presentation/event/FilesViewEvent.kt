/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.files.presentation.event

import androidx.paging.CombinedLoadStates
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.files.presentation.state.VolumeEntry
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink

interface FilesViewEvent {
    val onTopAppBarNavigation: () -> Unit
    val onSorting: (Sorting) -> Unit
    val onDriveLink: ((DriveLink) -> Unit)?
    val onLoadState: (CombinedLoadStates, Int) -> Unit
    val onErrorAction: () -> Unit get() = {}
    val onAppendErrorAction: () -> Unit get() = {}
    val onMoreOptions: (DriveLink) -> Unit get() = {}
    val onParentFolderOptions: () -> Unit get() = {}
    val onSelectedOptions: () -> Unit get() = {}
    val onAddFiles: () -> Unit get() = {}
    val onCancelUpload: (UploadFileLink) -> Unit get() = {}
    val onToggleLayout: () -> Unit get() = {}
    val onSelectDriveLink: (DriveLink) -> Unit get() = {}
    val onDeselectDriveLink: (DriveLink) -> Unit get() = {}
    val onBack: () -> Unit get() = {}
    val onTab: (VolumeEntry) -> Unit get() = {}
    val onSubscription: () -> Unit get() = {}
}
