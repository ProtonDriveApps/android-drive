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

package me.proton.android.drive.ui.viewstate

import kotlinx.coroutines.flow.Flow
import me.proton.android.drive.photos.presentation.state.AlbumsItem
import me.proton.core.drive.files.presentation.entry.OptionEntry

data class ShareMultiplePhotosOptionsViewState(
    val shareOptionsSectionTitleResId: Int,
    val shareOptions: List<OptionEntry<Unit>>,
    val sharedAlbumsSectionTitleResId: Int,
    val sharedAlbums: Flow<List<AlbumsItem.Listing>>,
)
