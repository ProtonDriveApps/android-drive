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

package me.proton.core.drive.sorting.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.SORTING_BY
import me.proton.core.drive.base.data.db.Column.SORTING_DIRECTION
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction

@Entity(
    primaryKeys = [USER_ID],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = [Column.Core.USER_ID],
            childColumns = [USER_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
)
data class SortingEntity(
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = SORTING_BY)
    val sortingBy: By,
    @ColumnInfo(name = SORTING_DIRECTION)
    val sortingDirection: Direction,
)
