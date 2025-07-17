/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.photo.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.photo.data.db.entity.TagsMigrationFileEntity
import me.proton.core.drive.photo.data.db.entity.TagsMigrationFileTagEntity

@Dao
abstract class TagsMigrationFileTagDao : BaseDao<TagsMigrationFileTagEntity>() {

    @Query(
        """
        SELECT * FROM TagsMigrationFileTagEntity
        WHERE user_id = :userId AND
            volume_id = :volumeId AND
            share_id = :shareId AND
            id = :fileId
        """
    )
    abstract suspend fun getTags(
        userId: UserId,
        volumeId: String,
        shareId: String,
        fileId: String
    ): List<TagsMigrationFileTagEntity>
}
