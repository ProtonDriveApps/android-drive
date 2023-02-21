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
package me.proton.core.drive.share.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.ADDRESS_ID
import me.proton.core.drive.base.data.db.Column.CREATION_TIME
import me.proton.core.drive.base.data.db.Column.FLAGS
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.KEY
import me.proton.core.drive.base.data.db.Column.LINK_ID
import me.proton.core.drive.base.data.db.Column.LOCKED
import me.proton.core.drive.base.data.db.Column.PASSPHRASE
import me.proton.core.drive.base.data.db.Column.PASSPHRASE_SIGNATURE
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.base.data.db.Column.VOLUME_ID
import me.proton.core.user.domain.entity.AddressId

@Entity(
    primaryKeys = [USER_ID, ID],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = [Column.Core.USER_ID],
            childColumns = [USER_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = [USER_ID]),
        Index(value = [VOLUME_ID]),
        Index(value = [LINK_ID]),
        Index(value = [ID], unique = true),
    ]
)
data class ShareEntity(
    @ColumnInfo(name = ID)
    val id: String,
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = VOLUME_ID)
    val volumeId: String,
    @ColumnInfo(name = ADDRESS_ID)
    val addressId: AddressId? = null,
    @ColumnInfo(name = FLAGS)
    val flags: Long,
    @ColumnInfo(name = LINK_ID)
    val linkId: String,
    @ColumnInfo(name = LOCKED)
    val isLocked: Boolean,
    @ColumnInfo(name = KEY)
    val key: String,
    @ColumnInfo(name = PASSPHRASE)
    val passphrase: String,
    @ColumnInfo(name = PASSPHRASE_SIGNATURE)
    val passphraseSignature: String,
    @ColumnInfo(name = CREATION_TIME)
    val creationTime: Long? = null,
) {
    companion object {
        const val PRIMARY_BIT = 1L
    }
}
