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

package me.proton.core.drive.documentsprovider.data.extension

import android.annotation.SuppressLint
import android.database.MatrixCursor
import android.os.Build
import android.provider.DocumentsContract
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.extension.requireFileId

@SuppressLint("InlinedApi")
internal fun UploadFileLink.addTo(cursor: MatrixCursor.RowBuilder) {
    val flags = DocumentsContract.Document.FLAG_SUPPORTS_DELETE
        .add {
            DocumentsContract.Document.FLAG_SUPPORTS_RENAME
        }.add {
            DocumentsContract.Document.FLAG_SUPPORTS_MOVE
        }.add {
            DocumentsContract.Document.FLAG_SUPPORTS_WRITE
        }.addIf(mimeType.startsWith("image/")) {
            DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
        }

    cursor.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentId(userId, requireFileId()).encode())
    cursor.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, name)
    cursor.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType)
    cursor.add(DocumentsContract.Document.COLUMN_FLAGS, flags)
}
