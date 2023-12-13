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
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.repository.ScanFolderRepository
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import javax.inject.Inject

class ContextScanFolderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configurationProvider: ConfigurationProvider,
) : ScanFolderRepository {
    override suspend operator fun invoke(bucketId: Int, timestamp: TimestampS?): List<BackupFile> =
        withContext(Dispatchers.IO) {
            val limit = configurationProvider.scanBackupPageSize
            val sort = MediaStore.MediaColumns.DATE_ADDED
            collectMedias(imageCollection, bucketId, limit, sort, timestamp) +
                    collectMedias(videoCollection, bucketId, limit, sort, timestamp)
        }

    private fun collectMedias(
        uri: Uri,
        bucketId: Int,
        limit: Int,
        sort: String,
        timestamp: TimestampS?,
    ): List<BackupFile> {
        val backupFiles: MutableList<BackupFile> = mutableListOf()
        var page = 0
        var count: Int
        do {
            val offset = page * limit
            page++
            val backupFileInPage: List<BackupFile> = context.contentResolver
                .query(uri, bucketId, sort, limit, offset, timestamp?.value)
                ?.use { cursor -> cursor.createMedia(bucketId, uri) }
                ?: emptyList()
            count = backupFileInPage.size
            backupFiles.addAll(backupFileInPage)
        } while (count > 0)
        return backupFiles
    }
}

private fun Cursor.createMedia(bucketId: Int, uri: Uri): List<BackupFile> {
    val listOfAllImages = mutableListOf<BackupFile>()
    if (count > 0) {
        val id = getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val dateAdded = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        val displayName = getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val size = getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val mimeType = getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        while (moveToNext()) {
            listOfAllImages.add(
                BackupFile(
                    bucketId = bucketId,
                    uriString = Uri.withAppendedPath(uri, getString(id))?.let { uriMedia ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            MediaStore.setRequireOriginal(uriMedia)
                        } else {
                            uriMedia
                        }
                    }.toString(),
                    mimeType = getString(mimeType),
                    name = getString(displayName),
                    hash = null,
                    size = getInt(size).bytes,
                    state = BackupFileState.IDLE,
                    date = TimestampS(getInt(dateAdded).toLong()),
                )
            )
        }
    }
    return listOfAllImages
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
private val projections by lazy {
    arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.MIME_TYPE,
    )
}

private fun ContentResolver.query(
    uri: Uri,
    bucketId: Int,
    sort: String,
    limit: Int,
    offset: Int,
    timestamp: Long?,
): Cursor? {
    val (selection, args) = if (timestamp != null) {
        "${MediaStore.MediaColumns.BUCKET_ID} LIKE ? AND ${MediaStore.MediaColumns.DATE_ADDED} >= ?" to
                arrayOf(bucketId.toString(), timestamp.toString())
    } else {
        "${MediaStore.MediaColumns.BUCKET_ID} LIKE ?" to
                arrayOf(bucketId.toString())
    }
    return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
        val bundle = Bundle().apply {
            // selection
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
            // sort
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(sort)
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
            // limit, offset
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        }
        query(
            uri,
            projections,
            bundle,
            null,
        )
    } else {
        query(
            uri,
            projections,
            selection,
            args,
            "$sort DESC LIMIT $limit OFFSET $offset",
        )
    }
}
