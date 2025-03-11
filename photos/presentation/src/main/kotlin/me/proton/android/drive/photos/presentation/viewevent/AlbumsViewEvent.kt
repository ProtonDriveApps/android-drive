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

package me.proton.android.drive.photos.presentation.viewevent

import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.photo.domain.entity.AlbumListing

interface AlbumsViewEvent {
    val onRefresh: () -> Unit get() = {}
    val onScroll: (Set<LinkId>) -> Unit get() = { _ -> }
    val onErrorAction: () -> Unit get() = {}
    val onTopAppBarNavigation: () -> Unit get() = {}
    val onDriveLinkAlbum: (DriveLink.Album) -> Unit get() = {}
    val onCreateNewAlbum: () -> Unit get() = {}
    val onFilterSelected: (AlbumListing.Filter) -> Unit get() = {}
}
