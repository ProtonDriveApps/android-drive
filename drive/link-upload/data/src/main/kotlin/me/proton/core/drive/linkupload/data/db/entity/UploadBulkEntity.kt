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
import androidx.room.PrimaryKey
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = [Column.Core.USER_ID],
            childColumns = [Column.USER_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [Column.USER_ID]),
        Index(value = [Column.VOLUME_ID]),
        Index(value = [Column.SHARE_ID]),
        Index(value = [Column.PARENT_ID]),
    ]
)
data class UploadBulkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Column.ID)
    val id: Long = 0,
    @ColumnInfo(name = Column.USER_ID)
    val userId: UserId,
    @ColumnInfo(name = Column.VOLUME_ID)
    val volumeId: String,
    @ColumnInfo(name = Column.SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = Column.PARENT_ID)
    val parentId: String,
    @ColumnInfo(name = Column.SHOULD_DELETE_SOURCE_URI)
    val shouldDeleteSourceUri: Boolean = false,
)
