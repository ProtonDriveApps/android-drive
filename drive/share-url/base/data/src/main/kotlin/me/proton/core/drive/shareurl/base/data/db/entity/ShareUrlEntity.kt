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
package me.proton.core.drive.shareurl.base.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column.CREATION_TIME
import me.proton.core.drive.base.data.db.Column.CREATOR_EMAIL
import me.proton.core.drive.base.data.db.Column.EXPIRATION_TIME
import me.proton.core.drive.base.data.db.Column.FLAGS
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.LAST_ACCESS_TIME
import me.proton.core.drive.base.data.db.Column.MAX_ACCESSES
import me.proton.core.drive.base.data.db.Column.NAME
import me.proton.core.drive.base.data.db.Column.NUMBER_OF_ACCESSES
import me.proton.core.drive.base.data.db.Column.PASSWORD
import me.proton.core.drive.base.data.db.Column.PERMISSIONS
import me.proton.core.drive.base.data.db.Column.PUBLIC_URL
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.SHARE_PASSPHRASE_KEY_PACKET
import me.proton.core.drive.base.data.db.Column.SHARE_PASSWORD_SALT
import me.proton.core.drive.base.data.db.Column.SRP_MODULUS_ID
import me.proton.core.drive.base.data.db.Column.SRP_VERIFIER
import me.proton.core.drive.base.data.db.Column.TOKEN
import me.proton.core.drive.base.data.db.Column.URL_PASSWORD_SALT
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.share.data.db.ShareEntity

@Entity(
    primaryKeys = [USER_ID, SHARE_ID, ID],
    foreignKeys = [
        ForeignKey(
            entity = ShareEntity::class,
            parentColumns = [USER_ID, ID],
            childColumns = [USER_ID, SHARE_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = [USER_ID]),
        Index(value = [SHARE_ID]),
        Index(value = [USER_ID, SHARE_ID]),
    ],
)
data class ShareUrlEntity(
    @ColumnInfo(name = ID)
    val id: String,
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = FLAGS)
    val flags: Long,
    @ColumnInfo(name = NAME)
    val name: String?,
    @ColumnInfo(name = TOKEN)
    val token: String,
    @ColumnInfo(name = CREATOR_EMAIL)
    val creatorEmail: String,
    @ColumnInfo(name = PERMISSIONS)
    val permissions: Long,
    @ColumnInfo(name = CREATION_TIME)
    val creationTime: Long,
    @ColumnInfo(name = EXPIRATION_TIME)
    val expirationTime: Long?,
    @ColumnInfo(name = LAST_ACCESS_TIME)
    val lastAccessTime: Long?,
    @ColumnInfo(name = MAX_ACCESSES)
    val maxAccesses: Long?,
    @ColumnInfo(name = NUMBER_OF_ACCESSES)
    val numberOfAccesses: Long,
    @ColumnInfo(name = URL_PASSWORD_SALT)
    val urlPasswordSalt: String,
    @ColumnInfo(name = SHARE_PASSWORD_SALT)
    val sharePasswordSalt: String,
    @ColumnInfo(name = SRP_VERIFIER)
    val srpVerifier: String,
    @ColumnInfo(name = SRP_MODULUS_ID)
    val srpModulusId: String,
    @ColumnInfo(name = PASSWORD)
    val encryptedUrlPassword: String,
    @ColumnInfo(name = SHARE_PASSPHRASE_KEY_PACKET)
    val sharePassphraseKeyPacket: String,
    @ColumnInfo(name = PUBLIC_URL, defaultValue = "")
    val publicUrl: String,
)
