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

package me.proton.core.drive.backup.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.BUCKET_ID
import me.proton.core.drive.base.data.db.Column.PARENT_ID
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.SYNC_TIME
import me.proton.core.drive.base.data.db.Column.UPDATE_TIME
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.link.data.db.entity.LinkEntity
import me.proton.core.drive.share.data.db.ShareEntity

@Entity(
    primaryKeys = [USER_ID, SHARE_ID, PARENT_ID, BUCKET_ID],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = [Column.Core.USER_ID],
            childColumns = [USER_ID],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShareEntity::class,
            parentColumns = [USER_ID, Column.ID],
            childColumns = [USER_ID, SHARE_ID],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LinkEntity::class,
            parentColumns = [USER_ID, SHARE_ID, Column.ID],
            childColumns = [USER_ID, SHARE_ID, PARENT_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
)
data class BackupFolderEntity(
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = PARENT_ID)
    val parentId: String,
    @ColumnInfo(name = BUCKET_ID)
    val bucketId: Int,
    @ColumnInfo(name = UPDATE_TIME)
    val updateTime: Long?,
    @ColumnInfo(name = SYNC_TIME, defaultValue = "NULL")
    val syncTime: Long?,
)
