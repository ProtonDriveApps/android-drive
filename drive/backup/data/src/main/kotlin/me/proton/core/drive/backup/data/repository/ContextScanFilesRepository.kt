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

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.util.coRunCatching
import javax.inject.Inject

class ContextScanFilesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed interface ScanResult {
        val uri: Uri

        data class Data(
            override val uri: Uri,
            val dateAdded: TimestampS?,
        ) : ScanResult

        data class NotFound(override val uri: Uri, val error: Throwable? = null) : ScanResult
    }

    suspend operator fun invoke(
        uris: List<Uri>,
    ): Result<List<ScanResult>> = coRunCatching(Dispatchers.IO) {
        uris.map { uri ->
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dateAdded = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                            .takeIf { columnIndex ->
                                columnIndex != -1
                            }?.let { columnIndex ->
                                cursor.getInt(columnIndex).toLong()
                            }
                        ScanResult.Data(uri, dateAdded?.let(::TimestampS))
                    } else {
                        ScanResult.NotFound(uri)
                    }
                } ?: ScanResult.NotFound(uri, IllegalStateException("Query returns null"))
            }.recoverCatching { error ->
                ScanResult.NotFound(uri, error)
            }.getOrThrow()
        }
    }
}
