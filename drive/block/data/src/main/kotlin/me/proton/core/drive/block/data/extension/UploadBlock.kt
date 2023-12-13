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
package me.proton.core.drive.block.data.extension

import me.proton.core.drive.block.data.api.entity.UploadBlockDto
import me.proton.core.drive.block.data.api.entity.UploadThumbnailDto
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.block.data.api.entity.VerifierDto
import me.proton.core.drive.linkupload.domain.entity.UploadBlock

fun UploadBlock.toUploadBlockDto() =
    UploadBlockDto(
        index = index,
        size = size.value,
        hash = hashSha256,
        encSignature = encSignature,
        verifier = verifierToken?.let { token -> VerifierDto(token) },
    )

fun UploadBlock.toUploadThumbnailDto() =
    UploadThumbnailDto(
        size = size.value,
        type = when (type) {
            Block.Type.THUMBNAIL_DEFAULT -> "1"
            Block.Type.THUMBNAIL_PHOTO -> "2"
            else -> error("Unexpected block type: $type")
        },
        hash = hashSha256,
    )
