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
import me.proton.core.drive.base.data.db.Column.CAPTURE_TIME
import me.proton.core.drive.base.data.db.Column.CONTENT_HASH
import me.proton.core.drive.base.data.db.Column.CONTENT_KEY_PACKET
import me.proton.core.drive.base.data.db.Column.CONTENT_KEY_PACKET_SIGNATURE
import me.proton.core.drive.base.data.db.Column.HAS_THUMBNAIL
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.LINK_ID
import me.proton.core.drive.base.data.db.Column.MAIN_PHOTO_LINK_ID
import me.proton.core.drive.base.data.db.Column.REVISION_ID
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.SIGNATURE_ADDRESS
import me.proton.core.drive.base.data.db.Column.THUMBNAIL_ID_DEFAULT
import me.proton.core.drive.base.data.db.Column.THUMBNAIL_ID_PHOTO
import me.proton.core.drive.base.data.db.Column.USER_ID

private const val PREFIX = "file"
private const val FILE_USER_ID = "${PREFIX}_$USER_ID"
private const val FILE_SHARE_ID = "${PREFIX}_$SHARE_ID"
private const val FILE_LINK_ID = "${PREFIX}_$LINK_ID"
private const val FILE_SIGNATURE_ADDRESS = "${PREFIX}_$SIGNATURE_ADDRESS"

@Entity(
    primaryKeys = [FILE_USER_ID, FILE_SHARE_ID, FILE_LINK_ID],
    foreignKeys = [
        ForeignKey(
            entity = LinkEntity::class,
            parentColumns = [USER_ID, SHARE_ID, ID],
            childColumns = [FILE_USER_ID, FILE_SHARE_ID, FILE_LINK_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = [FILE_SHARE_ID]),
        Index(value = [FILE_LINK_ID]),
        Index(value = [REVISION_ID]),
        Index(value = [FILE_USER_ID, FILE_SHARE_ID, FILE_LINK_ID]),
    ],
)
data class LinkFilePropertiesEntity(
    @ColumnInfo(name = FILE_USER_ID)
    val userId: UserId,
    @ColumnInfo(name = FILE_SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = FILE_LINK_ID)
    val linkId: String,
    @ColumnInfo(name = REVISION_ID)
    val activeRevisionId: String,
    @ColumnInfo(name = HAS_THUMBNAIL)
    val hasThumbnail: Boolean,
    @ColumnInfo(name = CONTENT_KEY_PACKET)
    val contentKeyPacket: String,
    @ColumnInfo(name = CONTENT_KEY_PACKET_SIGNATURE)
    val contentKeyPacketSignature: String? = null,
    @ColumnInfo(name = FILE_SIGNATURE_ADDRESS)
    val activeRevisionSignatureAddress: String? = null,
    @ColumnInfo(name = CAPTURE_TIME, defaultValue = "NULL")
    val photoCaptureTime: Long? = null,
    @ColumnInfo(name = CONTENT_HASH, defaultValue = "NULL")
    val photoContentHash: String? = null,
    @ColumnInfo(name = MAIN_PHOTO_LINK_ID, defaultValue = "NULL")
    val mainPhotoLinkId: String? = null,
    @ColumnInfo(name = THUMBNAIL_ID_DEFAULT)
    val defaultThumbnailId: String? = null,
    @ColumnInfo(name = THUMBNAIL_ID_PHOTO)
    val photoThumbnailId: String? = null,
) : LinkPropertiesEntity
