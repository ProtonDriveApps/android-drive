/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.backup.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.Images.ImageColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.drive.backup.domain.entity.BucketEntry
import me.proton.core.drive.backup.domain.repository.BucketRepository
import javax.inject.Inject


class ContextBucketRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : BucketRepository {
    override suspend fun getAll(): List<BucketEntry> =
        withContext(Dispatchers.IO) {
            loadBucketEntriesFromFilesTable(context.contentResolver)
        }

    // From https://android.googlesource.com/platform/packages/apps/Gallery2/+/master/src/com/android/gallery3d/data/BucketHelper.java
    private fun loadBucketEntriesFromFilesTable(
        resolver: ContentResolver,
    ): List<BucketEntry> = resolver.query(
        getFilesContentUri(),
        PROJECTION_BUCKET,
        null,
        null,
        "${FileColumns.DATE_MODIFIED} DESC"
    )?.use { cursor ->
        val entries = mutableListOf<BucketEntry>()
        while (cursor.moveToNext()) {
            val mediaType = cursor.getInt(INDEX_MEDIA_TYPE)
            if (mediaType == FileColumns.MEDIA_TYPE_IMAGE
                || mediaType == FileColumns.MEDIA_TYPE_VIDEO
            ) {
                val entry = BucketEntry(
                    bucketId = cursor.getInt(INDEX_BUCKET_ID),
                    bucketName = cursor.getString(INDEX_BUCKET_NAME),
                    lastItemUriString = Uri.withAppendedPath(
                        when (mediaType) {
                            FileColumns.MEDIA_TYPE_IMAGE -> imageCollection
                            FileColumns.MEDIA_TYPE_VIDEO -> videoCollection
                            else -> getFilesContentUri()
                        },
                        cursor.getString(INDEX_ID)
                    )?.let { uriMedia ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            MediaStore.setRequireOriginal(uriMedia)
                        } else {
                            uriMedia
                        }
                    }.toString()
                )
                if (!entries.contains(entry)) {
                    entries.add(entry)
                    entry
                } else {
                    entries.first { it == entry }
                }.also {
                    when (mediaType) {
                        FileColumns.MEDIA_TYPE_IMAGE -> it.imageCount++
                        FileColumns.MEDIA_TYPE_VIDEO -> it.videoCount++
                        else -> Unit
                    }
                }
            }
        }
        entries
    } ?: emptyList()

    private val imageCollection: Uri by lazy {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    }

    private val videoCollection: Uri by lazy {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    }

    private companion object {

        private val PROJECTION_BUCKET = arrayOf(
            ImageColumns.BUCKET_ID,
            FileColumns.MEDIA_TYPE,
            ImageColumns.BUCKET_DISPLAY_NAME,
            ImageColumns._ID
        )

        // The indices should match the above projections.
        private const val INDEX_BUCKET_ID = 0
        private const val INDEX_MEDIA_TYPE = 1
        private const val INDEX_BUCKET_NAME = 2
        private const val INDEX_ID = 3

        private const val EXTERNAL_MEDIA = "external"
        private fun getFilesContentUri(): Uri {
            return MediaStore.Files.getContentUri(EXTERNAL_MEDIA)
        }
    }

}
