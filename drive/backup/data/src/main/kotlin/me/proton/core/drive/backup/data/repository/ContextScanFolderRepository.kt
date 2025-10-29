/*
 * Copyright (c) 2023-2024 Proton AG.
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
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.ScanFolderRepository
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import javax.inject.Inject

class ContextScanFolderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configurationProvider: ConfigurationProvider,
) : ScanFolderRepository {
    override suspend operator fun invoke(backupFolder: BackupFolder): List<BackupFile> =
        withContext(Dispatchers.IO) {
            val limit = configurationProvider.scanBackupPageSize
            val sort = MediaStore.MediaColumns.DATE_ADDED
            collectMedias(imageCollection, backupFolder, limit, sort) +
                    collectMedias(videoCollection, backupFolder, limit, sort)
        }

    private fun collectMedias(
        uri: Uri,
        backupFolder: BackupFolder,
        limit: Int,
        sort: String,
    ): List<BackupFile> {
        val backupFiles: MutableList<BackupFile> = mutableListOf()
        var page = 0
        var count: Int
        do {
            val offset = page * limit
            page++
            val backupFileInPage: List<BackupFile> = context.contentResolver
                .query(
                    uri,
                    backupFolder.bucketId,
                    sort,
                    limit,
                    offset,
                    backupFolder.updateTime?.value,
                )
                ?.use { cursor -> cursor.createMedia(backupFolder, uri) }
                ?: emptyList()
            count = backupFileInPage.size
            backupFiles.addAll(backupFileInPage)
        } while (count > 0)
        return backupFiles
    }
}

private fun Cursor.createMedia(backupFolder: BackupFolder, uri: Uri): List<BackupFile> {
    val listOfAllImages = mutableListOf<BackupFile>()
    if (count > 0) {
        val id = getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val dateAdded = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        val dateModified = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
        val displayName = getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val size = getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val mimeType = getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        while (moveToNext()) {
            runCatching {
                listOfAllImages.add(
                    BackupFile(
                        bucketId = backupFolder.bucketId,
                        folderId = backupFolder.folderId,
                        uriString = Uri.withAppendedPath(
                            uri, getInt(id).toString()
                        )?.let { uriMedia ->
                            MediaStore.setRequireOriginal(uriMedia)
                        }.toString(),
                        mimeType = getStringOrThrow(mimeType),
                        name = getStringOrThrow(displayName),
                        hash = null,
                        size = getInt(size).bytes,
                        state = BackupFileState.IDLE,
                        date = TimestampS(getInt(dateAdded).toLong()),
                        lastModified = TimestampS(getInt(dateModified).toLong()),
                    )
                )
            }.onFailure { error ->
                error.log(BACKUP, "Cannot read row at position: $position, count: $count")
            }
        }
    }
    return listOfAllImages
}

private fun Cursor.getStringOrThrow(
    columnIndex: Int,
): String = when (val type = getType(columnIndex)) {
    Cursor.FIELD_TYPE_NULL -> error("Column $columnIndex is null")
    Cursor.FIELD_TYPE_STRING -> getString(columnIndex)
    else -> error("Column $columnIndex is not a string but $type")
}

private val imageCollection: Uri by lazy {
    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
}

private val videoCollection: Uri by lazy {
    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
}
private val projections by lazy {
    arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.DATE_MODIFIED,
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
