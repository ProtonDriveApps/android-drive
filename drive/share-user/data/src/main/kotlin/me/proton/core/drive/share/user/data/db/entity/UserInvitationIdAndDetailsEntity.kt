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
package me.proton.core.drive.share.user.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.DETAILS
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.base.data.db.Column.VOLUME_ID

@Entity(
    primaryKeys = [USER_ID, VOLUME_ID, SHARE_ID, ID],
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
    ]
)
data class UserInvitationIdAndDetailsEntity(
    @ColumnInfo(ID)
    val id: String,
    @ColumnInfo(USER_ID)
    val userId: UserId,
    @ColumnInfo(VOLUME_ID)
    val volumeId: String,
    @ColumnInfo(SHARE_ID)
    val shareId: String,
    @Embedded(prefix = "details_")
    val details: UserInvitationDetailsEntity?,

)
