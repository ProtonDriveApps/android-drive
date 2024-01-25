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
import androidx.room.Index
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.ATTEMPTS
import me.proton.core.drive.base.data.db.Column.BUCKET_ID
import me.proton.core.drive.base.data.db.Column.CREATION_TIME
import me.proton.core.drive.base.data.db.Column.HASH
import me.proton.core.drive.base.data.db.Column.LAST_MODIFIED
import me.proton.core.drive.base.data.db.Column.MIME_TYPE
import me.proton.core.drive.base.data.db.Column.NAME
import me.proton.core.drive.base.data.db.Column.PARENT_ID
import me.proton.core.drive.base.data.db.Column.PRIORITY
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.SIZE
import me.proton.core.drive.base.data.db.Column.STATE
import me.proton.core.drive.base.data.db.Column.URI
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.link.data.db.entity.LinkEntity
import me.proton.core.drive.share.data.db.ShareEntity

@Entity(
    primaryKeys = [USER_ID, SHARE_ID, PARENT_ID, URI],
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
        ForeignKey(
            entity = BackupFolderEntity::class,
            parentColumns = [USER_ID, SHARE_ID, PARENT_ID, BUCKET_ID],
            childColumns = [USER_ID, SHARE_ID, PARENT_ID, BUCKET_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [USER_ID, SHARE_ID, PARENT_ID, BUCKET_ID]),
        Index(value = [USER_ID, SHARE_ID, PARENT_ID, URI]),
        Index(value = [USER_ID, SHARE_ID, PARENT_ID, STATE]),
        Index(value = [USER_ID, SHARE_ID, PARENT_ID, BUCKET_ID, STATE]),
    ]
)
data class BackupFileEntity(
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = PARENT_ID)
    val parentId: String,
    @ColumnInfo(name = BUCKET_ID)
    val bucketId: Int,
    @ColumnInfo(name = URI)
    val uriString: String,
    @ColumnInfo(name = MIME_TYPE)
    val mimeType: String,
    @ColumnInfo(name = NAME)
    val name: String,
    @ColumnInfo(name = HASH)
    val hash: String,
    @ColumnInfo(name = SIZE)
    val size: Long,
    @ColumnInfo(name = STATE, defaultValue = "IDLE")
    val state: BackupFileState = BackupFileState.IDLE,
    @ColumnInfo(name = CREATION_TIME)
    val createTime: Long,
    @ColumnInfo(name = PRIORITY, defaultValue = "${Long.MAX_VALUE}")
    val uploadPriority: Long,
    @ColumnInfo(name = ATTEMPTS, defaultValue = "0")
    val attempts: Long = 0,
    @ColumnInfo(name = LAST_MODIFIED)
    val lastModified: Long? = null,
)
