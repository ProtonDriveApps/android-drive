/*
 * Copyright (c) 2021-2023 Proton AG.
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
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.LINK_ID
import me.proton.core.drive.base.data.db.Column.NODE_HASH_KEY
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.USER_ID

private const val PREFIX = "folder"
private const val FOLDER_USER_ID = "${PREFIX}_$USER_ID"
private const val FOLDER_SHARE_ID = "${PREFIX}_$SHARE_ID"
private const val FOLDER_LINK_ID = "${PREFIX}_$LINK_ID"

@Entity(
    primaryKeys = [FOLDER_USER_ID, FOLDER_SHARE_ID, FOLDER_LINK_ID],
    foreignKeys = [
        ForeignKey(
            entity = LinkEntity::class,
            parentColumns = [USER_ID, SHARE_ID, ID],
            childColumns = [FOLDER_USER_ID, FOLDER_SHARE_ID, FOLDER_LINK_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = [FOLDER_SHARE_ID]),
        Index(value = [FOLDER_LINK_ID]),
        Index(value = [FOLDER_USER_ID, FOLDER_SHARE_ID, FOLDER_LINK_ID]),
    ],
)
data class LinkFolderPropertiesEntity(
    @ColumnInfo(name = FOLDER_USER_ID)
    val userId: UserId,
    @ColumnInfo(name = FOLDER_SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = FOLDER_LINK_ID)
    val linkId: String,
    @ColumnInfo(name = NODE_HASH_KEY)
    val nodeHashKey: String,
) : LinkPropertiesEntity
