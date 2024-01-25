/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.upload.data.extension

import android.database.Cursor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toTimestampMs
import me.proton.core.drive.base.domain.extension.bytes

val Cursor.name: String? get() = getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { index -> index >= 0 }
    ?.let { index ->
        getString(index)
    }

val Cursor.size: Bytes? get() = getColumnIndex(OpenableColumns.SIZE).takeIf { index -> index >= 0 }
    ?.let { index ->
        getLong(index).bytes
    }

val Cursor.lastModified: TimestampMs? get() {
    val documentLastModified =
        getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
    val mediaDateModified =
        getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

    return if (documentLastModified != -1) {
        TimestampMs(getLong(documentLastModified))
    } else if (mediaDateModified != 1) {
        TimestampS(getLong(mediaDateModified)).toTimestampMs()
    } else {
        null
    }
        ?.takeIf { lastModified -> lastModified.value >= 0 }
}
