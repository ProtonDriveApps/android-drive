/*
 * Copyright (c) 2025 Proton AG.
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
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId

data class AlbumViewState(
    val name: String,
    val details: String,
    val listContentState: ListContentState,
    val coverLinkId: FileId? = null,
    val isRefreshEnabled: Boolean = false,
    val topBarActions: Flow<Set<Action>> = emptyFlow(),
    val selected: Flow<Set<LinkId>> = emptyFlow(),
    val inMultiselect: Boolean = false,
    val showActions: Boolean = true,
    @DrawableRes val navigationIconResId: Int? = null,
    val title: String? = null,
)
