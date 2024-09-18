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

package me.proton.core.drive.linkupload.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.drive.linkupload.data.db.entity.RawBlockEntity

@Dao
abstract class RawBlockDao : BaseDao<RawBlockEntity>() {

    @Query(
        """
        SELECT * FROM RawBlockEntity WHERE
            upload_link_id = :uploadLinkId
        ORDER BY `index` ASC
        LIMIT :limit OFFSET :offset
    """
    )
    abstract suspend fun get(uploadLinkId: Long, limit: Int, offset: Int): List<RawBlockEntity>

    @Query(
        """
        DELETE FROM RawBlockEntity WHERE
            upload_link_id = :uploadLinkId AND `index` = :index
        """
    )
    abstract suspend fun delete(uploadLinkId: Long, index: Long)

    @Query(
        """
        DELETE FROM RawBlockEntity WHERE
            upload_link_id = :uploadLinkId
        """
    )
    abstract suspend fun deleteAll(uploadLinkId: Long)
}
