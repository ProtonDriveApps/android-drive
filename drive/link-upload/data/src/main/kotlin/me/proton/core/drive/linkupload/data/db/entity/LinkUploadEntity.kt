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
import androidx.room.Index
import androidx.room.PrimaryKey
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.CONTENT_KEY_PACKET
import me.proton.core.drive.base.data.db.Column.CONTENT_KEY_PACKET_SIGNATURE
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.LAST_MODIFIED
import me.proton.core.drive.base.data.db.Column.LINK_ID
import me.proton.core.drive.base.data.db.Column.MANIFEST_SIGNATURE
import me.proton.core.drive.base.data.db.Column.MEDIA_RESOLUTION_HEIGHT
import me.proton.core.drive.base.data.db.Column.MEDIA_RESOLUTION_WIDTH
import me.proton.core.drive.base.data.db.Column.MIME_TYPE
import me.proton.core.drive.base.data.db.Column.NAME
import me.proton.core.drive.base.data.db.Column.NODE_KEY
import me.proton.core.drive.base.data.db.Column.NODE_PASSPHRASE
import me.proton.core.drive.base.data.db.Column.NODE_PASSPHRASE_SIGNATURE
import me.proton.core.drive.base.data.db.Column.PARENT_ID
import me.proton.core.drive.base.data.db.Column.REVISION_ID
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.SHOULD_DELETE_SOURCE_URI
import me.proton.core.drive.base.data.db.Column.SIZE
import me.proton.core.drive.base.data.db.Column.STATE
import me.proton.core.drive.base.data.db.Column.URI
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.base.data.db.Column.VOLUME_ID
import me.proton.core.drive.linkupload.domain.entity.UploadState

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = [Column.Core.USER_ID],
            childColumns = [USER_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [USER_ID]),
        Index(value = [VOLUME_ID]),
        Index(value = [SHARE_ID]),
        Index(value = [LINK_ID]),
        Index(value = [REVISION_ID]),
        Index(value = [PARENT_ID]),
    ]
)
data class LinkUploadEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    val id: Long = 0,
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = VOLUME_ID)
    val volumeId: String,
    @ColumnInfo(name = SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = PARENT_ID)
    val parentId: String,
    @ColumnInfo(name = LINK_ID)
    val linkId: String = "",
    @ColumnInfo(name = REVISION_ID)
    val revisionId: String = "",
    @ColumnInfo(name = NAME)
    val name: String,
    @ColumnInfo(name = MIME_TYPE)
    val mimeType: String = "",
    @ColumnInfo(name = NODE_KEY)
    val nodeKey: String = "",
    @ColumnInfo(name = NODE_PASSPHRASE)
    val nodePassphrase: String = "",
    @ColumnInfo(name = NODE_PASSPHRASE_SIGNATURE)
    val nodePassphraseSignature: String = "",
    @ColumnInfo(name = CONTENT_KEY_PACKET)
    val contentKeyPacket: String = "",
    @ColumnInfo(name = CONTENT_KEY_PACKET_SIGNATURE)
    val contentKeyPacketSignature: String = "",
    @ColumnInfo(name = MANIFEST_SIGNATURE)
    val manifestSignature: String = "",
    @ColumnInfo(name = STATE)
    val state: UploadState,
    @ColumnInfo(name = SIZE, defaultValue = "NULL")
    val size: Long?,
    @ColumnInfo(name = LAST_MODIFIED)
    val lastModified: Long? = null,
    @ColumnInfo(name = URI, defaultValue = "NULL")
    val uri: String? = null,
    @ColumnInfo(name = SHOULD_DELETE_SOURCE_URI, defaultValue = "false")
    val shouldDeleteSourceUri: Boolean = false,
    @ColumnInfo(name = MEDIA_RESOLUTION_WIDTH, defaultValue = "NULL")
    val mediaResolutionWidth: Long? = null,
    @ColumnInfo(name = MEDIA_RESOLUTION_HEIGHT, defaultValue = "NULL")
    val mediaResolutionHeight: Long? = null,
)
