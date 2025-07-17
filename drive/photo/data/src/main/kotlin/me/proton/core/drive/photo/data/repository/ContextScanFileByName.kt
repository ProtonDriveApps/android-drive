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

package me.proton.core.drive.photo.data.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.Images.ImageColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.drive.photo.domain.usecase.ScanFileByName
import javax.inject.Inject

class ContextScanFileByName @Inject constructor(
    @ApplicationContext private val context: Context,
) : ScanFileByName {
    override suspend fun invoke(name: String): List<String> = withContext(Dispatchers.IO) {
        context.contentResolver
            .query(
                getFilesContentUri(),
                PROJECTION,
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                arrayOf(name),
                "${FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val uris = mutableListOf<String?>()
                while (cursor.moveToNext()) {
                    uris += cursor.extractUri()
                }
                uris.filterNotNull()
            }.orEmpty()
    }

    private fun Cursor.extractUri(): String? {
        val mediaType = getInt(INDEX_MEDIA_TYPE)
        return if (mediaType == FileColumns.MEDIA_TYPE_IMAGE
            || mediaType == FileColumns.MEDIA_TYPE_VIDEO
        ) {
            Uri.withAppendedPath(
                when (mediaType) {
                    FileColumns.MEDIA_TYPE_IMAGE -> imageCollection
                    FileColumns.MEDIA_TYPE_VIDEO -> videoCollection
                    else -> getFilesContentUri()
                },
                getString(INDEX_ID)
            )?.let { uriMedia ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.setRequireOriginal(uriMedia)
                } else {
                    uriMedia
                }
            }.toString()
        } else {
            null
        }
    }

    private val imageCollection: Uri by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    private val videoCollection: Uri by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
    }

    private companion object {

        private val PROJECTION = arrayOf(
            FileColumns.MEDIA_TYPE,
            ImageColumns._ID
        )

        // The indices should match the above projections.
        private const val INDEX_MEDIA_TYPE = 0
        private const val INDEX_ID = 1

        private const val EXTERNAL_MEDIA = "external"
        private fun getFilesContentUri(): Uri {
            return MediaStore.Files.getContentUri(EXTERNAL_MEDIA)
        }
    }

}
