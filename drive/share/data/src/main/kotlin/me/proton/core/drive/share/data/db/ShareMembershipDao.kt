/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.share.data.db

import androidx.room.Dao
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column

@Dao
abstract class ShareMembershipDao : BaseDao<ShareMembershipEntity>() {


    @Query(
        """
        SELECT id FROM ShareMembershipEntity WHERE user_id = :userId 
        """
    )
    abstract suspend fun getAllIds(userId: UserId): List<String>


    @Query(
        """
        SELECT permissions FROM ShareMembershipEntity WHERE 
            user_id = :userId AND 
            share_id IN (:shareIds)
        """
    )
    abstract suspend fun getPermissions(userId: UserId, shareIds: List<String>): List<Long>

    companion object {
        const val LINK_JOIN_STATEMENT = """
            LEFT JOIN ShareMembershipEntity ON
                LinkEntity.${Column.SHARE_URL_SHARE_ID} = ShareMembershipEntity.${Column.SHARE_ID} AND
                LinkEntity.${Column.USER_ID} = ShareMembershipEntity.${Column.USER_ID}
        """
    }
}
