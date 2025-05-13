/*
 * Copyright (c) 2023 Proton AG.
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
import me.proton.core.drive.photo.data.db.entity.TaggedRelatedPhotoEntity

@Dao
abstract class TaggedRelatedPhotoDao : BaseDao<TaggedRelatedPhotoEntity>() {

    @Query("""
        SELECT * FROM TaggedRelatedPhotoEntity
            WHERE
                user_id = :userId AND
                volume_id = :volumeId AND
                main_photo_link_id = :mainLinkId
            ORDER BY capture_time ASC, id ASC
            LIMIT :limit OFFSET :offset
    """)
    abstract suspend fun getPhotoListings(
        userId: UserId,
        volumeId: String,
        mainLinkId: String,
        limit: Int,
        offset: Int,
    ): List<TaggedRelatedPhotoEntity>
}
