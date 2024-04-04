/*
 * Copyright (c) 2021-2024 Proton AG.
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
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.ATTRIBUTES
import me.proton.core.drive.base.data.db.Column.CREATION_TIME
import me.proton.core.drive.base.data.db.Column.EXPIRATION_TIME
import me.proton.core.drive.base.data.db.Column.HASH
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.IS_SHARED
import me.proton.core.drive.base.data.db.Column.LAST_MODIFIED
import me.proton.core.drive.base.data.db.Column.MIME_TYPE
import me.proton.core.drive.base.data.db.Column.NAME
import me.proton.core.drive.base.data.db.Column.NAME_SIGNATURE_EMAIL
import me.proton.core.drive.base.data.db.Column.NUMBER_OF_ACCESSES
import me.proton.core.drive.base.data.db.Column.PARENT_ID
import me.proton.core.drive.base.data.db.Column.PERMISSIONS
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.SHARE_URL_EXPIRATION_TIME
import me.proton.core.drive.base.data.db.Column.SHARE_URL_ID
import me.proton.core.drive.base.data.db.Column.SHARE_URL_SHARE_ID
import me.proton.core.drive.base.data.db.Column.SIGNATURE_ADDRESS
import me.proton.core.drive.base.data.db.Column.SIZE
import me.proton.core.drive.base.data.db.Column.STATE
import me.proton.core.drive.base.data.db.Column.TRASHED_TIME
import me.proton.core.drive.base.data.db.Column.TYPE
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.base.data.db.Column.X_ATTR
import me.proton.core.drive.share.data.db.ShareEntity

@Entity(
    primaryKeys = [USER_ID, SHARE_ID, ID],
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
            childColumns = [USER_ID, SHARE_ID, PARENT_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = [USER_ID]),
        Index(value = [SHARE_ID]),
        Index(value = [PARENT_ID]),
        Index(value = [ID]),
        Index(value = [USER_ID, SHARE_ID]),
        Index(value = [USER_ID, ID]),
        Index(value = [USER_ID, SHARE_ID, PARENT_ID]),
    ],
)
data class LinkEntity(
    @ColumnInfo(name = ID)
    val id: String,
    @ColumnInfo(name = SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = PARENT_ID)
    val parentId: String? = null,
    @ColumnInfo(name = TYPE)
    val type: Long,
    @ColumnInfo(name = NAME)
    val name: String,
    @ColumnInfo(name = NAME_SIGNATURE_EMAIL)
    val nameSignatureEmail: String?,
    @ColumnInfo(name = HASH)
    val hash: String,
    @ColumnInfo(name = STATE)
    val state: Long,
    @ColumnInfo(name = EXPIRATION_TIME)
    val expirationTime: Long? = null,
    @ColumnInfo(name = SIZE)
    val size: Long,
    @ColumnInfo(name = MIME_TYPE)
    val mimeType: String,
    @ColumnInfo(name = ATTRIBUTES)
    val attributes: Long,
    @ColumnInfo(name = PERMISSIONS)
    val permissions: Long,
    @ColumnInfo(name = Column.NODE_KEY)
    val nodeKey: String,
    @ColumnInfo(name = Column.NODE_PASSPHRASE)
    val nodePassphrase: String,
    @ColumnInfo(name = Column.NODE_PASSPHRASE_SIGNATURE)
    val nodePassphraseSignature: String,
    @ColumnInfo(name = SIGNATURE_ADDRESS)
    val signatureAddress: String,
    @ColumnInfo(name = CREATION_TIME)
    val creationTime: Long,
    @ColumnInfo(name = LAST_MODIFIED)
    val lastModified: Long,
    @ColumnInfo(name = TRASHED_TIME)
    val trashedTime: Long? = null,
    @ColumnInfo(name = IS_SHARED)
    val shared: Long,
    @ColumnInfo(name = NUMBER_OF_ACCESSES)
    val numberOfAccesses: Long,
    @ColumnInfo(name = SHARE_URL_EXPIRATION_TIME)
    val shareUrlExpirationTime: Long?,
    @ColumnInfo(name = X_ATTR)
    val xAttr: String? = null,
    @ColumnInfo(name = SHARE_URL_SHARE_ID, defaultValue = "NULL")
    val sharingDetailsShareId: String? = null,
    @ColumnInfo(name = SHARE_URL_ID, defaultValue = "NULL")
    val shareUrlId: String? = null,
)
