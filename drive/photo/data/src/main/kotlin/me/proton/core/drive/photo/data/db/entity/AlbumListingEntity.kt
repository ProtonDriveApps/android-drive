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

package me.proton.core.drive.photo.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.COUNT
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.IS_SHARED
import me.proton.core.drive.base.data.db.Column.LAST_ACTIVITY_TIME
import me.proton.core.drive.base.data.db.Column.LINK_ID
import me.proton.core.drive.base.data.db.Column.LOCKED
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.base.data.db.Column.VOLUME_ID
import me.proton.core.drive.share.data.db.ShareEntity

@Entity(
    primaryKeys = [USER_ID, VOLUME_ID, SHARE_ID, ID],
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
    ],
    indices = [
        Index(value = [USER_ID]),
        Index(value = [VOLUME_ID]),
        Index(value = [SHARE_ID]),
        Index(value = [ID]),
        Index(value = [USER_ID, VOLUME_ID]),
        Index(value = [USER_ID, VOLUME_ID, SHARE_ID]),
    ],
)
data class AlbumListingEntity(
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = VOLUME_ID)
    val volumeId: String,
    @ColumnInfo(name = SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = ID)
    val albumId: String,
    @ColumnInfo(name = LOCKED)
    val locked: Boolean,
    @ColumnInfo(name = COUNT)
    val photoCount: Long,
    @ColumnInfo(name = LAST_ACTIVITY_TIME)
    val lastActivityTime: Long,
    @ColumnInfo(name = LINK_ID)
    val coverLinkId: String? = null,
    @ColumnInfo(name = IS_SHARED)
    val isShared: Boolean = false,
)
