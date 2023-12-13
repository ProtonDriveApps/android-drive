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

import android.content.ContentResolver.SCHEME_FILE
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.extensionOrEmpty
import me.proton.core.drive.base.domain.extension.size
import me.proton.core.drive.base.domain.provider.MimeTypeProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.upload.domain.resolver.UriResolver
import java.io.File
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

class FileUriResolver(
    private val mimeTypeProvider: MimeTypeProvider,
    private val coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
) : UriResolver {

    override suspend fun <T> useInputStream(uriString: String, block: suspend (InputStream) -> T): T? =
        uri(uriString)?.path?.let { path ->
            File(path).inputStream().use { fileInputStream ->
                block(fileInputStream)
            }
        }

    override suspend fun getName(uriString: String): String? = coRunCatching(coroutineContext) {
        uri(uriString)?.lastPathSegment
    }.getOrNull()

    override suspend fun getSize(uriString: String): Bytes? = coRunCatching(coroutineContext) {
        uri(uriString)?.path?.let { path ->
            File(path).size
        }
    }.getOrNull()

    override suspend fun getMimeType(uriString: String): String? = coRunCatching(coroutineContext) {
        getName(uriString)?.let { name ->
            mimeTypeProvider.getMimeTypeFromExtension(name.extensionOrEmpty) ?: UriResolver.DEFAULT_MIME_TYPE
        }
    }.getOrNull()

    override suspend fun getLastModified(uriString: String): TimestampMs? = coRunCatching(coroutineContext) {
        uri(uriString)?.path?.let { path ->
            TimestampMs(File(path).lastModified())
        }
    }.getOrNull()

    private fun uri(uriString: String?): Uri? = Uri.parse(uriString).also { uri -> require(uri.scheme == SCHEME_FILE) }
}
