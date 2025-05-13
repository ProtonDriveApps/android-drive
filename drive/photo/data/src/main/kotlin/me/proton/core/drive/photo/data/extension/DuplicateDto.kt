/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.photo.data.extension

import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.photo.data.api.response.DuplicateDto
import me.proton.core.drive.photo.domain.entity.PhotoDuplicate

fun DuplicateDto.toPhotoDuplicate(parentId: ParentId) = PhotoDuplicate(
    parentId = parentId,
    hash = hash,
    contentHash = contentHash,
    linkId = linkId?.let { id -> FileId(parentId.shareId, id) },
    linkState = state?.toState(),
    revisionId = revisionId,
    clientUid = clientUid
)

private fun Long.toState() = when (this) {
    LinkDto.STATE_DRAFT -> Link.State.DRAFT
    LinkDto.STATE_ACTIVE -> Link.State.ACTIVE
    LinkDto.STATE_TRASHED -> Link.State.TRASHED
    LinkDto.STATE_DELETED -> Link.State.DELETED
    LinkDto.STATE_RESTORING -> Link.State.RESTORING
    else -> throw IllegalArgumentException("Unknown state $this")
}
