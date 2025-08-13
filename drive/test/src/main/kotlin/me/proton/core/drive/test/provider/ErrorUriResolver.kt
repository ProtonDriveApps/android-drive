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

package me.proton.core.drive.test.provider

import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.upload.domain.resolver.UriResolver
import java.io.InputStream

open class ErrorUriResolver : UriResolver {
    override suspend fun <T> useInputStream(
        uriString: String,
        block: suspend (InputStream) -> T
    ): T? = error("Not yet implemented")

    override suspend fun exists(uriString: String): Boolean = error("Not yet implemented")

    override suspend fun getName(uriString: String): String? = error("Not yet implemented")

    override suspend fun getSize(uriString: String): Bytes? = error("Not yet implemented")

    override suspend fun getMimeType(uriString: String): String? = error("Not yet implemented")

    override suspend fun getLastModified(uriString: String): TimestampMs? =
        error("Not yet implemented")

    override suspend fun getUriInfo(uriString: String): UriResolver.UriInfo? =
        error("Not yet implemented")

    override suspend fun getParentName(uriString: String): String? =
        error("Not yet implemented")

}

