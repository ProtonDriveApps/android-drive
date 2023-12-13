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
package me.proton.core.drive.linkupload.domain.factory

import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.linkupload.domain.entity.UploadBlock
import java.io.File

interface UploadBlockFactory {
    fun create(
        index: Long,
        block: File,
        hashSha256: ByteArray,
        encSignature: String,
        rawSize: Bytes,
        size: Bytes,
        token: String,
        type: Block.Type,
        verifierToken: String?,
    ): UploadBlock

    fun create(
        index: Long,
        url: String,
        hashSha256: String,
        encSignature: String,
        rawSize: Bytes,
        size: Bytes,
        token: String,
        type: Block.Type,
        verifierToken: String?,
    ): UploadBlock
}
