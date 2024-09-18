/*
 * Copyright (c) 2024 Proton AG.
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
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.INDEX
import me.proton.core.drive.base.data.db.Column.NAME
import me.proton.core.drive.base.data.db.Column.UPLOAD_LINK_ID

@Entity(
    primaryKeys = [UPLOAD_LINK_ID, INDEX],
    foreignKeys = [
        ForeignKey(
            entity = LinkUploadEntity::class,
            parentColumns = [ID],
            childColumns = [UPLOAD_LINK_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = [UPLOAD_LINK_ID]),
    ],
)
data class RawBlockEntity(
    @ColumnInfo(name = UPLOAD_LINK_ID)
    val uploadLinkId: Long,
    @ColumnInfo(name = INDEX)
    val index: Long,
    @ColumnInfo(name = NAME)
    val name: String,
)
