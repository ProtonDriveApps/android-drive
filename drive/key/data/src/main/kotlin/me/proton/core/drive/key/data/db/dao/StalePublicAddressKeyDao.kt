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

package me.proton.core.drive.key.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.key.data.db.entity.StalePublicAddressKeyEntity

@Dao
abstract class StalePublicAddressKeyDao : BaseDao<StalePublicAddressKeyEntity>() {

    @Query(
        """
            SELECT EXISTS(SELECT * FROM StalePublicAddressKeyEntity WHERE user_id = :userId AND email = :email)
        """
    )
    abstract suspend fun hasEmail(userId: UserId, email: String): Boolean

    @Query(
        """
            DELETE FROM StalePublicAddressKeyEntity WHERE user_id = :userId AND email = :email
        """
    )
    abstract suspend fun deleteAll(userId: UserId, email: String)
}
