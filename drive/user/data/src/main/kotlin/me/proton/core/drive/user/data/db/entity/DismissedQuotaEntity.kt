/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.user.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.LEVEL
import me.proton.core.drive.base.data.db.Column.MAX_SPACE
import me.proton.core.drive.base.data.db.Column.UPDATE_TIME
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.user.domain.entity.QuotaLevel

@Entity(
    primaryKeys = [USER_ID, LEVEL, MAX_SPACE],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = [Column.Core.USER_ID],
            childColumns = [USER_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
)
data class DismissedQuotaEntity(
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = LEVEL)
    val level: QuotaLevel,
    @ColumnInfo(name = MAX_SPACE)
    val maxSpace: Long,
    @ColumnInfo(name = UPDATE_TIME)
    val timestampS: Long,
)
