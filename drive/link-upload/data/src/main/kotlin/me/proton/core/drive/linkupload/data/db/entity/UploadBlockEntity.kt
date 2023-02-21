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
package me.proton.core.drive.linkupload.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import me.proton.core.drive.base.data.db.Column.ENCRYPTED_SIGNATURE
import me.proton.core.drive.base.data.db.Column.HASH
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.INDEX
import me.proton.core.drive.base.data.db.Column.RAW_SIZE
import me.proton.core.drive.base.data.db.Column.SIZE
import me.proton.core.drive.base.data.db.Column.TOKEN
import me.proton.core.drive.base.data.db.Column.UPLOAD_LINK_ID
import me.proton.core.drive.base.data.db.Column.URL

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
)
data class UploadBlockEntity(
    @ColumnInfo(name = UPLOAD_LINK_ID)
    val uploadLinkId: Long,
    @ColumnInfo(name = INDEX)
    val index: Long,
    @ColumnInfo(name = SIZE)
    val size: Long,
    @ColumnInfo(name = ENCRYPTED_SIGNATURE)
    val encryptedSignature: String,
    @ColumnInfo(name = HASH)
    val hash: String,
    @ColumnInfo(name = TOKEN)
    val uploadToken: String,
    @ColumnInfo(name = URL)
    val url: String,
    @ColumnInfo(name = RAW_SIZE, defaultValue = "0")
    val rawSize: Long,
)
