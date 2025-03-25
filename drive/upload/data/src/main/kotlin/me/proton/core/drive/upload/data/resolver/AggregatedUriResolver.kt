/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.upload.data.resolver

import android.net.Uri
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.upload.domain.resolver.UriResolver
import java.io.InputStream
import javax.inject.Inject

class AggregatedUriResolver @Inject constructor(
    private val uriResolvers: @JvmSuppressWildcards Map<String, UriResolver>,
) : UriResolver {
    override val schemes: Set<String> get() = uriResolvers.keys

    override suspend fun <T> useInputStream(uriString: String, block: suspend (InputStream) -> T): T? =
        uriResolvers.forScheme(uriString).useInputStream(uriString, block)

    override suspend fun getName(uriString: String): String? =
        uriResolvers.forScheme(uriString).getName(uriString)

    override suspend fun getSize(uriString: String): Bytes? =
        uriResolvers.forScheme(uriString).getSize(uriString)

    override suspend fun getMimeType(uriString: String): String =
        uriResolvers.forScheme(uriString).getMimeType(uriString) ?: UriResolver.DEFAULT_MIME_TYPE

    override suspend fun getLastModified(uriString: String): TimestampMs? =
        uriResolvers.forScheme(uriString).getLastModified(uriString)

    override suspend fun getParentName(uriString: String): String? =
        uriResolvers.forScheme(uriString).getParentName(uriString)

    override suspend fun getUriInfo(uriString: String): UriResolver.UriInfo? =
        uriResolvers.forScheme(uriString).getUriInfo(uriString)

    private fun Map<String, UriResolver>.forScheme(uriString: String): UriResolver =
        get(Uri.parse(uriString).scheme)
            ?: throw IllegalStateException("No Uri resolver for given Uri scheme ${Uri.parse(uriString).scheme}")
}
