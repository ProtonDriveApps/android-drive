/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.test.resolver

import android.net.Uri
import androidx.core.net.toFile
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.upload.domain.resolver.UriResolver
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import javax.inject.Inject

class TestUriResolver @Inject constructor() : UriResolver {
    override suspend fun <T> useInputStream(
        uriString: String,
        block: suspend (InputStream) -> T,
    ): T? = if (uriString == "test://missing") {
        throw FileNotFoundException("Cannot found file with uri: $uriString")
    } else {
        block(ByteArrayInputStream(uriString.toByteArray()))
    }

    override suspend fun exists(uriString: String): Boolean = true

    override suspend fun getName(uriString: String): String = uriString

    override suspend fun getSize(uriString: String): Bytes = uriString.length.bytes

    override suspend fun getMimeType(uriString: String): String = "application/octet-stream"

    override suspend fun getLastModified(uriString: String): TimestampMs = TimestampMs()

    override suspend fun getParentName(
        uriString: String,
    ): String = requireNotNull(Uri.parse(uriString).toFile().parentFile).name

    override suspend fun getUriInfo(uriString: String): UriResolver.UriInfo = UriResolver.UriInfo(
        name = getName(uriString),
        size = getSize(uriString),
        mimeType = getMimeType(uriString),
        lastModified = getLastModified(uriString)
    )
}
