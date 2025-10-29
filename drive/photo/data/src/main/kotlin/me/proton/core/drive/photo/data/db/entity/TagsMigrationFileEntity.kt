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
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column.CAPTURE_TIME
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.MIME_TYPE
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.STATE
import me.proton.core.drive.base.data.db.Column.URI
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.base.data.db.Column.VOLUME_ID
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.share.data.db.ShareEntity

@Entity(
    primaryKeys = [USER_ID, VOLUME_ID, SHARE_ID, ID],
    foreignKeys = [
        ForeignKey(
            entity = ShareEntity::class,
            parentColumns = [USER_ID, ID],
            childColumns = [USER_ID, SHARE_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = [USER_ID, VOLUME_ID]),
        Index(value = [USER_ID, SHARE_ID]),
        Index(value = [USER_ID, SHARE_ID, ID]),
        Index(value = [USER_ID, VOLUME_ID, SHARE_ID, ID]),
    ],
)
data class TagsMigrationFileEntity(
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = VOLUME_ID)
    val volumeId: String,
    @ColumnInfo(name = SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = ID)
    val linkId: String,
    @ColumnInfo(name = CAPTURE_TIME)
    val captureTime: Long,
    @ColumnInfo(name = STATE)
    val state: TagsMigrationFile.State,
    @ColumnInfo(name = URI)
    val uriString: String?,
    @ColumnInfo(name = MIME_TYPE)
    val mimeType: String? = null,
)
