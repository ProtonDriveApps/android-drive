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

package me.proton.core.drive.backup.data.db

import androidx.room.TypeConverter
import me.proton.core.drive.backup.domain.entity.BackupNetworkType

class BackupNetworkTypeConverters {
    // ANY was renamed from CONNECTED; kept as the stored string to avoid a migration.
    @TypeConverter
    fun fromBackupNetworkTypeToString(value: BackupNetworkType): String = when (value) {
        BackupNetworkType.ANY -> "CONNECTED"
        BackupNetworkType.UNMETERED -> "UNMETERED"
        BackupNetworkType.UNCONFIRMED -> "UNCONFIRMED"
    }

    @TypeConverter
    fun fromStringToBackupNetworkType(value: String): BackupNetworkType = when (value) {
        "CONNECTED" -> BackupNetworkType.ANY
        "UNMETERED" -> BackupNetworkType.UNMETERED
        "UNCONFIRMED" -> BackupNetworkType.UNCONFIRMED
        else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: $value")
    }
}
