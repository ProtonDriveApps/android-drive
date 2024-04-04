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
package me.proton.core.drive.files.presentation.state

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink

@Immutable
data class FilesViewState(
    val title: String?, // title to show on files screen, if title is null fallback to [titleResId]
    @StringRes val titleResId: Int,
    val isTitleEncrypted: Boolean = false,
    val sorting: Sorting,
    @DrawableRes val navigationIconResId: Int,
    val drawerGesturesEnabled: Boolean,
    val listContentState: ListContentState,
    val listContentAppendingState: ListContentAppendingState,
    val showHeader: Boolean = true,
    val isGrid: Boolean = false,
    val isSelectingDestination: Boolean = false,
    val isClickEnabled: (DriveLink) -> Boolean = { true },
    val isTextEnabled: (DriveLink) -> Boolean = defaultIsTextEnabled,
    val getUploadProgress: (uploadFileLink: UploadFileLink) -> Flow<Percentage>? = { null },
    val isRefreshEnabled: Boolean = true,
    val selected: Flow<Set<LinkId>> = emptyFlow(),
    val topBarActions: Flow<Set<Action>> = emptyFlow(),
    val isDriveLinkMoreOptionsEnabled: Boolean = true,
    val notificationDotVisible: Boolean = false,
) {

    companion object {
        val defaultIsTextEnabled: (DriveLink) -> Boolean = { link ->
                !(link.isTrashed && link.isProcessing)
            }
    }
}
