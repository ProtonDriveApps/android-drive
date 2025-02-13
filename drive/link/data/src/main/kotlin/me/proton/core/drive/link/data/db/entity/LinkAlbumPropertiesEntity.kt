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

package me.proton.core.drive.link.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column.COUNT
import me.proton.core.drive.base.data.db.Column.COVER_LINK_ID
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.LAST_ACTIVITY_TIME
import me.proton.core.drive.base.data.db.Column.LINK_ID
import me.proton.core.drive.base.data.db.Column.LOCKED
import me.proton.core.drive.base.data.db.Column.NODE_HASH_KEY
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.USER_ID

private const val PREFIX = "album"
private const val ALBUM_USER_ID = "${PREFIX}_$USER_ID"
private const val ALBUM_SHARE_ID = "${PREFIX}_$SHARE_ID"
private const val ALBUM_LINK_ID = "${PREFIX}_$LINK_ID"
private const val ALBUM_NODE_HASH_KEY = "${PREFIX}_$NODE_HASH_KEY"

@Entity(
    primaryKeys = [ALBUM_USER_ID, ALBUM_SHARE_ID, ALBUM_LINK_ID],
    foreignKeys = [
        ForeignKey(
            entity = LinkEntity::class,
            parentColumns = [USER_ID, SHARE_ID, ID],
            childColumns = [ALBUM_USER_ID, ALBUM_SHARE_ID, ALBUM_LINK_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = [ALBUM_SHARE_ID]),
        Index(value = [ALBUM_LINK_ID]),
    ],
)
data class LinkAlbumPropertiesEntity(
    @ColumnInfo(name = ALBUM_USER_ID)
    val userId: UserId,
    @ColumnInfo(name = ALBUM_SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = ALBUM_LINK_ID)
    val linkId: String,
    @ColumnInfo(name = ALBUM_NODE_HASH_KEY)
    val nodeHashKey: String,
    @ColumnInfo(name = LOCKED)
    val locked: Boolean,
    @ColumnInfo(name = LAST_ACTIVITY_TIME)
    val lastActivityTime: Long,
    @ColumnInfo(name = COUNT)
    val photoCount: Long,
    @ColumnInfo(name = COVER_LINK_ID)
    val coverLinkId: String? = null,
) : LinkPropertiesEntity
