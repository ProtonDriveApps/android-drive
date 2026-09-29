/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.base.data.db

import androidx.room.TypeConverter
import me.proton.drive.sdk.entity.NodeUid
import me.proton.drive.sdk.entity.ParentNodeUid
import me.proton.drive.sdk.entity.RevisionUid

class DriveUidConverters {
    @TypeConverter
    fun fromNodeUidToString(value: NodeUid?): String? = value?.value

    @TypeConverter
    fun fromStringToNodeUid(value: String?): NodeUid? = value?.let { NodeUid(it) }

    @TypeConverter
    fun fromParentNodeUidToString(value: ParentNodeUid?): String? = value?.value

    @TypeConverter
    fun fromStringToParentNodeUid(value: String?): ParentNodeUid? = value?.let { ParentNodeUid(it) }

    @TypeConverter
    fun fromRevisionUidToString(value: RevisionUid?): String? = value?.value

    @TypeConverter
    fun fromStringToRevisionUid(value: String?): RevisionUid? = value?.let { RevisionUid(it) }
}
