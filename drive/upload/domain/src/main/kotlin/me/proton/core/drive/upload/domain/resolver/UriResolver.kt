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
package me.proton.core.drive.upload.domain.resolver

import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampMs
import java.io.InputStream

interface UriResolver {
    suspend fun <T> useInputStream(uriString: String, block: suspend (InputStream) -> T): T?
    suspend fun getName(uriString: String): String?
    suspend fun getSize(uriString: String): Bytes?
    suspend fun getMimeType(uriString: String): String?
    suspend fun getLastModified(uriString: String): TimestampMs?


    companion object {
        const val DEFAULT_MIME_TYPE = "application/octet-stream"
    }
}
