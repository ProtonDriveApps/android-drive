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
package me.proton.core.drive.linkupload.data.factory

import android.net.Uri
import android.util.Base64
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.linkupload.domain.entity.UploadBlock
import me.proton.core.drive.linkupload.domain.factory.UploadBlockFactory
import java.io.File
import javax.inject.Inject

class UploadBlockFactoryImpl @Inject constructor() : UploadBlockFactory {
    override fun create(
        index: Long,
        block: File,
        hashSha256: ByteArray,
        encSignature: String,
        rawSize: Bytes,
        size: Bytes,
        token: String,
        verifierToken: String?,
    ): UploadBlock =
        UploadBlock(
            index = index,
            url = Uri.fromFile(block).toString(),
            hashSha256 = Base64.encodeToString(hashSha256, Base64.NO_WRAP),
            encSignature = encSignature,
            rawSize = rawSize,
            size = size,
            token = token,
            file = block,
            verifierToken = verifierToken,
        )

    override fun create(
        index: Long,
        url: String,
        hashSha256: String,
        encSignature: String,
        rawSize: Bytes,
        size: Bytes,
        token: String,
        verifierToken: String?,
    ): UploadBlock =
        UploadBlock(
            index = index,
            url = url,
            hashSha256 = hashSha256,
            encSignature = encSignature,
            rawSize = rawSize,
            size = size,
            token = token,
            file = File(requireNotNull(Uri.parse(url)?.path)),
            verifierToken = verifierToken,
        )
}
