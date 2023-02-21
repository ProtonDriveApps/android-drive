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
package me.proton.core.drive.volume.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.CREATION_TIME
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.MAX_SPACE
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.STATE
import me.proton.core.drive.base.data.db.Column.USED_SPACE
import me.proton.core.drive.base.data.db.Column.USER_ID

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
        Index(value = [SHARE_ID]),
        Index(value = [ID]),
    ]
)
data class VolumeEntity(
    @ColumnInfo(name = ID)
    val id: String,
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = CREATION_TIME)
    val creationTime: Long,
    @ColumnInfo(name = MAX_SPACE)
    val maxSpace: Long?,
    @ColumnInfo(name = USED_SPACE)
    val usedSpace: Long,
    @ColumnInfo(name = STATE)
    val state: Long
)
