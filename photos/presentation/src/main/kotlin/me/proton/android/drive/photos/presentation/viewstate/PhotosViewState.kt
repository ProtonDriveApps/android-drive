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

package me.proton.android.drive.photos.presentation.viewstate

import androidx.annotation.DrawableRes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.files.presentation.state.ListContentState
import me.proton.core.drive.link.domain.entity.LinkId

data class PhotosViewState(
    val title: String,
    @DrawableRes val navigationIconResId: Int,
    val topBarActions: Flow<Set<Action>> = emptyFlow(),
    val listContentState: ListContentState,
    val showEmptyList: Boolean?,
    val showPhotosStateIndicator: Boolean,
    val showPhotosStateBanner: Boolean,
    val backupStatusViewState: PhotosStatusViewState?,
    val selected: Flow<Set<LinkId>> = emptyFlow(),
    val isRefreshEnabled: Boolean = true,
    val notificationDotVisible: Boolean = false,
)
