/*
 * Copyright (c) 2025 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.ALBUM_ID
import me.proton.core.drive.base.data.db.Column.ALBUM_SHARE_ID
import me.proton.core.drive.base.data.db.Column.CAPTURE_TIME
import me.proton.core.drive.base.data.db.Column.CONTENT_HASH
import me.proton.core.drive.base.data.db.Column.HASH
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.LINK_ID
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.link.data.db.entity.LinkEntity
import me.proton.core.drive.share.data.db.ShareEntity

@Entity(
    primaryKeys = [USER_ID, SHARE_ID, LINK_ID],
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
        ForeignKey(
            entity = LinkEntity::class,
            parentColumns = [USER_ID, SHARE_ID, ID],
            childColumns = [USER_ID, ALBUM_SHARE_ID, ALBUM_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = [USER_ID]),
        Index(value = [ALBUM_ID]),
        Index(value = [USER_ID, ALBUM_ID]),
        Index(value = [USER_ID, ALBUM_SHARE_ID, ALBUM_ID]),
        Index(value = [USER_ID, SHARE_ID, LINK_ID, ALBUM_SHARE_ID, ALBUM_ID], unique = true),
    ],
)
data class AddToAlbumEntity(
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = LINK_ID)
    val linkId: String,
    @ColumnInfo(name = ALBUM_SHARE_ID)
    val albumShareId: String? = null,
    @ColumnInfo(name = ALBUM_ID)
    val albumId: String? = null,
    @ColumnInfo(name = CAPTURE_TIME)
    val captureTime: Long,
    @ColumnInfo(name = HASH)
    val hash: String?,
    @ColumnInfo(name = CONTENT_HASH)
    val contentHash: String?,
)
