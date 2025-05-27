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
package me.proton.core.drive.upload.data.resolver

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.extensionOrEmpty
import me.proton.core.drive.base.domain.log.LogTag.UPLOAD
import me.proton.core.drive.base.domain.provider.MimeTypeProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.upload.data.extension.bucketDisplayName
import me.proton.core.drive.upload.data.extension.lastModified
import me.proton.core.drive.upload.data.extension.log
import me.proton.core.drive.upload.data.extension.name
import me.proton.core.drive.upload.data.extension.size
import me.proton.core.drive.upload.domain.resolver.UriResolver
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

@SuppressLint("Range")
class ContentUriResolver(
    private val applicationContext: Context,
    private val mimeTypeProvider: MimeTypeProvider,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : UriResolver {
    override val schemes: Set<String> get() = setOf(SCHEME_CONTENT)

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun <T> useInputStream(uriString: String, block: suspend (InputStream) -> T): T? =
        applicationContext.contentResolver.openInputStream(Uri.parse(uriString))?.use { inputStream ->
            block(inputStream)
        }

    override suspend fun exists(uriString: String) = withContentResolver(uriString) { uri ->
        query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                "${MediaStore.MediaColumns.IS_TRASHED} = 0"
            } else {
                null
            },
            null,
            null
        )?.use { cursor ->
            cursor.count > 0
        }
    } ?: false

    override suspend fun getName(uriString: String): String? = withContentResolver(uriString) { uri ->
        query(uri, null, null, null, null)?.use { cursor ->
            takeIf { cursor.moveToFirst() }?.let {
                cursor.name
            }
        }
    }

    override suspend fun getSize(uriString: String): Bytes? = withContentResolver(uriString) { uri ->
        query(uri, null, null, null, null)?.use { cursor ->
            takeIf { cursor.moveToFirst() }?.let {
                cursor.size
            }
        }
    }

    override suspend fun getMimeType(
        uriString: String,
    ): String? = withContentResolver(uriString) { uri ->
        getType(uri) ?: getName(uriString)?.let { name ->
            mimeTypeProvider.getMimeTypeFromExtension(name.extensionOrEmpty)
                ?: UriResolver.DEFAULT_MIME_TYPE
        }
    }

    override suspend fun getLastModified(
        uriString: String,
    ): TimestampMs? = withContentResolver(uriString) { uri ->
        query(uri, null, null, null, null)?.use { cursor ->
            takeIf { cursor.moveToFirst() }?.let {
                cursor.lastModified
            }
        }
    }

    override suspend fun getParentName(
        uriString: String,
    ): String? = withContentResolver(uriString) { uri ->
        query(uri, null, null, null, null)?.use { cursor ->
            takeIf { cursor.moveToFirst() }?.let {
                cursor.bucketDisplayName
            }
        }
    }

    override suspend fun getUriInfo(
        uriString: String
    ): UriResolver.UriInfo? = withContentResolver(uriString) { uri ->
        query(uri, null, null, null, null)?.use { cursor ->
            takeIf { cursor.moveToFirst() }?.let {
                UriResolver.UriInfo(
                    name = cursor.name,
                    size = cursor.size,
                    mimeType = getMimeType(uriString),
                    lastModified = cursor.lastModified,
                )
            }
        }
    }

    private suspend fun<T> withContentResolver(
        uriString: String,
        block: suspend ContentResolver.(Uri) -> T,
    ): T? = coRunCatching(coroutineContext) {
        with (applicationContext.contentResolver) {
            val uri = Uri.parse(uriString)
            runCatching { takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            block(uri)
        }
    }.onFailure { error ->
        error.log(UPLOAD, "Error while using content provider for: $uriString")
    }.getOrNull()
}
