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
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.CREATE_TIME
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.INVITEE_EMAIL
import me.proton.core.drive.base.data.db.Column.INVITER_EMAIL
import me.proton.core.drive.base.data.db.Column.PERMISSIONS
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.SIGNATURE
import me.proton.core.drive.base.data.db.Column.STATE
import me.proton.core.drive.base.data.db.Column.USER_ID
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
    ],
    indices = [
        Index(value = [USER_ID]),
        Index(value = [USER_ID, SHARE_ID]),
        Index(value = [ID]),
    ]
)
data class ShareExternalInvitationEntity(
    @ColumnInfo(ID)
    val id: String,
    @ColumnInfo(USER_ID)
    val userId: UserId,
    @ColumnInfo(SHARE_ID)
    val shareId: String,
    @ColumnInfo(INVITER_EMAIL)
    val inviterEmail: String,
    @ColumnInfo(INVITEE_EMAIL)
    val inviteeEmail: String,
    @ColumnInfo(PERMISSIONS)
    val permissions: Long,
    @ColumnInfo(SIGNATURE)
    val signature: String,
    @ColumnInfo(STATE)
    val state: Long,
    @ColumnInfo(CREATE_TIME)
    val createTime: Long,
)
