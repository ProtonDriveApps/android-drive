/*
 * Copyright (c) 2022-2024 Proton AG.
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

package me.proton.core.drive.linkupload.domain.extension

import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.observability.domain.metrics.common.Initiator

val UploadFileLink.fileId get() = linkId?.let { FileId(shareId, linkId) }

fun UploadFileLink.requireFileId() = requireNotNull(fileId)

val UploadFileLink.sizeOrZero get() = size ?: 0.bytes

val UploadFileLink.isFileEmpty get() = size != null && size.value == 0L

fun UploadFileLink.toInitiator(): Initiator = when (priority) {
    in 1..UploadFileLink.USER_PRIORITY -> Initiator.explicit
    else -> Initiator.background
}
