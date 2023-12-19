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
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.drive.backup.domain.repository.CountLibraryItemsRepository
import javax.inject.Inject


class ContextCountLibraryItemsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : CountLibraryItemsRepository {
    override suspend fun getItemsCount(bucketIds: List<Int>): Int =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            bucketIds.sumOf { bucketId ->
                resolver.countItemsForBucket(bucketId)
            }
        }

    private fun ContentResolver.countItemsForBucket(
        bucketId: Int,
    ): Int = query(
        getFilesContentUri(),
        PROJECTION,
        "${MediaStore.MediaColumns.BUCKET_ID} LIKE ?",
        arrayOf(bucketId.toString()),
        "${FileColumns.DATE_MODIFIED} DESC"
    )?.use { cursor ->
        var count = 0
        while (cursor.moveToNext()) {
            val mediaType = cursor.getInt(INDEX_MEDIA_TYPE)
            if (mediaType == FileColumns.MEDIA_TYPE_IMAGE
                || mediaType == FileColumns.MEDIA_TYPE_VIDEO
            ) {
                count++
            }
        }
        count
    } ?: 0

    private companion object {

        private val PROJECTION = arrayOf(
            FileColumns.MEDIA_TYPE,
        )

        // The indices should match the above projections.
        private const val INDEX_MEDIA_TYPE = 0

        private const val EXTERNAL_MEDIA = "external"
        private fun getFilesContentUri(): Uri {
            return MediaStore.Files.getContentUri(EXTERNAL_MEDIA)
        }
    }

}
