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

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.repository.TagRepository
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class RemovePhotoTag @Inject constructor(
    private val repository: TagRepository,
    private val getShare: GetShare,
    private val updateEventAction: UpdateEventAction,
) {
    suspend operator fun invoke(fileId: FileId, tags: List<PhotoTag>) = coRunCatching {
        val share = getShare(fileId.shareId).toResult().getOrThrow()
        updateEventAction(fileId.userId, share.volumeId) {
            repository.deleteTags(share.volumeId, fileId, tags)
        }
    }
}
