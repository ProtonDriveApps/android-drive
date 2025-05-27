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
import me.proton.core.drive.base.domain.entity.toTimestampMs
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.extension.userId

@SuppressLint("InlinedApi")
internal fun DriveLink.addTo(cursor: MatrixCursor.RowBuilder) {

    val flags = DocumentsContract.Document.FLAG_SUPPORTS_DELETE
        .add {
            DocumentsContract.Document.FLAG_SUPPORTS_RENAME
        }.addIfAbove(Build.VERSION_CODES.N) {
            DocumentsContract.Document.FLAG_SUPPORTS_MOVE
        }.addIfElse(
            condition = this is Folder,
            matched = {
                DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
            },
            unmatched = {
                DocumentsContract.Document.FLAG_SUPPORTS_WRITE
            }
        ).addIf(this is DriveLink.File && hasThumbnail) {
            DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
        }

    cursor.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentId(userId, id).encode())
    cursor.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, name)
    cursor.add(DocumentsContract.Document.COLUMN_SIZE, size.value)
    cursor.add(
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        if (this is Folder) DocumentsContract.Document.MIME_TYPE_DIR else mimeType
    )
    cursor.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, lastModified.toTimestampMs().value)
    cursor.add(DocumentsContract.Document.COLUMN_FLAGS, flags)
}
