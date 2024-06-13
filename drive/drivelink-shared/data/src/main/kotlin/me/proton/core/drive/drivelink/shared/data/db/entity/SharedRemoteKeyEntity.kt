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

package me.proton.core.drive.drivelink.shared.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import me.proton.core.drive.base.data.db.Column.ANCHOR_ID
import me.proton.core.drive.base.data.db.Column.HAS_MORE
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.KEY

@Entity
data class SharedRemoteKeyEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    val id: Long,
    @ColumnInfo(name = KEY)
    val key: String,
    @ColumnInfo(name = ANCHOR_ID)
    val anchorId: String,
    @ColumnInfo(name = HAS_MORE)
    val hasMore: Boolean,
)
