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
package me.proton.android.drive.lock.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.proton.android.drive.lock.data.db.entity.LockEntity
import me.proton.android.drive.lock.domain.entity.AppLockType
import me.proton.core.data.room.db.BaseDao

@Dao
abstract class LockDao : BaseDao<LockEntity>() {
    @Query("""
        SELECT EXISTS(SELECT * FROM LockEntity WHERE type = :type)
    """)
    abstract suspend fun hasLock(type: AppLockType): Boolean

    @Query("""
        SELECT * FROM LockEntity WHERE type = :type LIMIT 1
    """)
    abstract suspend fun getLock(type: AppLockType): LockEntity

    @Query("""
        DELETE FROM LockEntity WHERE type = :type
    """)
    abstract suspend fun deleteLock(type: AppLockType)
}
