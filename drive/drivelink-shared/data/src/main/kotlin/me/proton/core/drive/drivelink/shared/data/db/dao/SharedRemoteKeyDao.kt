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

package me.proton.core.drive.drivelink.shared.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.drive.drivelink.shared.data.db.entity.SharedRemoteKeyEntity

@Dao
abstract class SharedRemoteKeyDao : BaseDao<SharedRemoteKeyEntity>() {

    @Query("""
        SELECT * FROM SharedRemoteKeyEntity
        WHERE
            `key` = :key
        ORDER BY id ASC
        LIMIT 1
    """)
    abstract suspend fun getLastRemoteKey(key: String): SharedRemoteKeyEntity?

    @Query(
        """
            DELETE FROM SharedRemoteKeyEntity WHERE `key` = :key
        """
    )
    abstract suspend fun deleteKeys(key: String)
}
