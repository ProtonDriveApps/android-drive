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

package me.proton.core.drive.photo.domain.usecase

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class DeleteAlbum @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val updateEventAction: UpdateEventAction,
) {
    suspend operator fun invoke(
        volumeId: VolumeId,
        albumId: AlbumId,
        deleteAlbumPhotos: Boolean = false,
    ) = coRunCatching {
        updateEventAction(albumId.userId, volumeId) {
            albumRepository.deleteAlbum(
                volumeId = volumeId,
                albumId = albumId,
                deleteAlbumPhotos = deleteAlbumPhotos,
            )
        }
    }
}
