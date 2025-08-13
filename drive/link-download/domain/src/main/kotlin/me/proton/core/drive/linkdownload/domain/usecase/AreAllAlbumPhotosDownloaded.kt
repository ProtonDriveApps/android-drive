/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.linkdownload.domain.usecase

import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.linkdownload.domain.repository.LinkDownloadRepository
import javax.inject.Inject

class AreAllAlbumPhotosDownloaded @Inject constructor(
    private val repository: LinkDownloadRepository,
) {

    suspend operator fun invoke(albumId: AlbumId): Boolean =
        repository.areAllAlbumPhotosDownloaded(
            albumId = albumId,
        )
}
