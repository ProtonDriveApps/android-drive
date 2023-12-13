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
package me.proton.core.drive.file.base.data.extension

import me.proton.core.drive.file.base.data.api.entity.RevisionDto
import me.proton.core.drive.file.base.domain.entity.Revision
import me.proton.core.drive.file.base.domain.entity.RevisionState

fun RevisionDto.toRevisionInfo() =
    Revision(
        id = id,
        revisionSize = revisionSize,
        thumbnailSize = thumbnailSize,
        blocks = blocks.map { blockDto -> blockDto.toBlock() },
        manifestSignature = manifestSignature,
        signatureAddress = signatureAddress,
        state = state.toRevisionState()
    )

private fun Long.toRevisionState(): RevisionState? {
    return when(this){
        0L -> RevisionState.DRAFT
        1L -> RevisionState.ACTIVE
        2L -> RevisionState.OBSOLETE
        4L -> RevisionState.DELETED
        else -> null
    }
}
