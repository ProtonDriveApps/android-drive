/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.linktrash.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.drive.link.data.db.LinkDao.Companion.PROPERTIES_ENTITIES_JOIN_STATEMENT
import me.proton.core.drive.link.data.db.entity.LinkWithPropertiesEntity
import me.proton.core.drive.linktrash.data.db.entity.TrashWorkEntity

@Dao
abstract class TrashWorkDao : BaseDao<TrashWorkEntity>() {

    @Query("SELECT EXISTS(SELECT * FROM TrashWorkEntity WHERE work_id = :workId)")
    abstract suspend fun hasWorkId(workId: String): Boolean

    @Query("SELECT * FROM $TRASH_WORK_WITH_LINKS WHERE work_id = :workId")
    abstract suspend fun getAllLinkWithProperties(workId: String): List<LinkWithPropertiesEntity>

    @Query("DELETE FROM TrashWorkEntity WHERE work_id = :workId")
    abstract suspend fun deleteAll(workId: String)

    companion object {
        const val TRASH_WORK_WITH_LINKS = """
            TrashWorkEntity INNER JOIN LinkEntity
                ON LinkEntity.user_id = TrashWorkEntity.user_id AND LinkEntity.share_id = TrashWorkEntity.share_id AND
                LinkEntity.id = TrashWorkEntity.link_id
            $PROPERTIES_ENTITIES_JOIN_STATEMENT 
        """
    }
}
