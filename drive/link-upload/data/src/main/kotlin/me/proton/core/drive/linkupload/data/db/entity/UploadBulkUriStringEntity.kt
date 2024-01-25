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
package me.proton.core.drive.linkupload.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.KEY
import me.proton.core.drive.base.data.db.Column.LAST_MODIFIED
import me.proton.core.drive.base.data.db.Column.MIME_TYPE
import me.proton.core.drive.base.data.db.Column.NAME
import me.proton.core.drive.base.data.db.Column.SIZE
import me.proton.core.drive.base.data.db.Column.UPLOAD_BULK_ID
import me.proton.core.drive.base.data.db.Column.URI

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = UploadBulkEntity::class,
            parentColumns = [Column.ID],
            childColumns = [UPLOAD_BULK_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [UPLOAD_BULK_ID]),
    ]
)
data class UploadBulkUriStringEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = KEY)
    val key: Long = 0,
    @ColumnInfo(name = UPLOAD_BULK_ID)
    val uploadBulkId: Long,
    @ColumnInfo(name = URI)
    val uri: String,
    @ColumnInfo(name = NAME)
    val name: String? = null,
    @ColumnInfo(name = MIME_TYPE)
    val mimeType: String? = null,
    @ColumnInfo(name = SIZE)
    val size: Long? = null,
    @ColumnInfo(name = LAST_MODIFIED)
    val lastModified: Long? = null,
)
