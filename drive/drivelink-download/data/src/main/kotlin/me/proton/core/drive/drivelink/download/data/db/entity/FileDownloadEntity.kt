/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.drivelink.download.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.LAST_FETCH_TIMESTAMP
import me.proton.core.drive.base.data.db.Column.LINK_ID
import me.proton.core.drive.base.data.db.Column.NUMBER_OF_RETRIES
import me.proton.core.drive.base.data.db.Column.PARENT_ID
import me.proton.core.drive.base.data.db.Column.PRIORITY
import me.proton.core.drive.base.data.db.Column.RETRYABLE
import me.proton.core.drive.base.data.db.Column.REVISION_ID
import me.proton.core.drive.base.data.db.Column.RUN_AT
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.STATE
import me.proton.core.drive.base.data.db.Column.TAG
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.base.data.db.Column.VOLUME_ID
import me.proton.core.drive.drivelink.download.domain.entity.DownloadFileLink
import me.proton.core.drive.link.data.db.entity.LinkEntity
import me.proton.core.drive.share.data.db.ShareEntity

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = [Column.Core.USER_ID],
            childColumns = [USER_ID],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShareEntity::class,
            parentColumns = [USER_ID, ID],
            childColumns = [USER_ID, SHARE_ID],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LinkEntity::class,
            parentColumns = [USER_ID, SHARE_ID, ID],
            childColumns = [USER_ID, SHARE_ID, LINK_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = [USER_ID, VOLUME_ID, SHARE_ID, LINK_ID, REVISION_ID], unique = true),
        Index(value = [USER_ID]),
        Index(value = [PRIORITY]),
        Index(value = [USER_ID, STATE]),
    ],
)
data class FileDownloadEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(ID)
    val id: Long,
    @ColumnInfo(LINK_ID)
    val fileId: String,
    @ColumnInfo(USER_ID)
    val userId: UserId,
    @ColumnInfo(SHARE_ID)
    val shareId: String,
    @ColumnInfo(VOLUME_ID)
    val volumeId: String,
    @ColumnInfo(REVISION_ID)
    val revisionId: String,
    @ColumnInfo(PRIORITY)
    val priority: Long,
    @ColumnInfo(RETRYABLE)
    val retryable: Boolean,
    @ColumnInfo(STATE)
    val state: DownloadFileLink.State = DownloadFileLink.State.IDLE,
    @ColumnInfo(PARENT_ID)
    val parentId: String? = null,
    @ColumnInfo(NUMBER_OF_RETRIES)
    val numberOfRetries: Int = 0,
    @ColumnInfo(RUN_AT)
    val lastRunTimestamp: Long? = null,
)
