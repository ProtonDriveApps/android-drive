/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.android.drive.lock.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.android.drive.lock.domain.entity.AppLockType
import me.proton.core.drive.base.data.db.Column.KEY
import me.proton.core.drive.base.data.db.Column.PASSPHRASE
import me.proton.core.drive.base.data.db.Column.TYPE

@Entity(
    primaryKeys = [PASSPHRASE],
    foreignKeys = [
        ForeignKey(
            entity = AppLockEntity::class,
            parentColumns = [KEY],
            childColumns = [KEY],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [KEY]),
    ],
)
data class LockEntity(
    @ColumnInfo(name = PASSPHRASE)
    val appKeyPassphrase: String,
    @ColumnInfo(name = KEY)
    val appKey: String,
    @ColumnInfo(name = TYPE)
    val type: AppLockType,
)
