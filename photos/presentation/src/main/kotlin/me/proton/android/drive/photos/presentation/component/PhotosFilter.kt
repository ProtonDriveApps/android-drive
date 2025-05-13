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

package me.proton.android.drive.photos.presentation.component

import androidx.compose.runtime.Composable
import me.proton.android.drive.photos.presentation.viewstate.PhotosFilter
import me.proton.core.drive.base.presentation.component.TagGroup
import me.proton.core.drive.link.domain.entity.PhotoTag

@Composable
fun PhotosFilter(
    filters: List<PhotosFilter>,
    onFilterSelected: (PhotoTag?) -> Unit,
) {
    TagGroup(filters.map { it.tagViewState }) { tagViewState ->
        filters.firstOrNull { filter ->
            filter.tagViewState == tagViewState
        }?.filter.let(onFilterSelected)
    }
}
