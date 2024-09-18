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

package me.proton.core.drive.key.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.crypto.common.pgp.Armored
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column.Core
import me.proton.core.drive.base.data.db.Column.EMAIL
import me.proton.core.drive.base.data.db.Column.KEY
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.key.data.entity.PublicAddressKeyDataEntity

@Entity(
    primaryKeys = [EMAIL, KEY],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = [Core.USER_ID],
            childColumns = [USER_ID],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PublicAddressKeyDataEntity::class,
            parentColumns = [EMAIL, Core.PUBLIC_KEY],
            childColumns = [EMAIL, KEY],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = [EMAIL]),
    ]
)
data class StalePublicAddressKeyEntity(
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = EMAIL)
    val email: String,
    @ColumnInfo(name = KEY)
    val key: Armored,
)
