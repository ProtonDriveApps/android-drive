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
package me.proton.core.drive.linkupload.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.drive.base.data.db.Column

@Entity(
    primaryKeys = [Column.UPLOAD_BULK_ID, Column.URI],
    foreignKeys = [
        ForeignKey(
            entity = UploadBulkEntity::class,
            parentColumns = [Column.ID],
            childColumns = [Column.UPLOAD_BULK_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [Column.UPLOAD_BULK_ID]),
    ]
)
data class UploadBulkUriStringEntity(
    @ColumnInfo(name = Column.UPLOAD_BULK_ID)
    val id: Long,
    @ColumnInfo(name = Column.URI)
    val uri: String,
)
