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
package me.proton.core.drive.share.user.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.user.data.db.entity.UserInvitationIdEntity

@Dao
abstract class UserInvitationIdDao : BaseDao<UserInvitationIdEntity>() {

    @Query(
        """SELECT EXISTS(
            SELECT * FROM UserInvitationIdEntity
            WHERE user_id = :userId
        )"""
    )
    abstract suspend fun hasInvitations(userId: UserId): Boolean

    @Query(
        """
            SELECT COUNT(id) FROM UserInvitationIdEntity
            WHERE
                user_id = :userId AND
                type IN (:types) OR (:includeNullType AND type IS NULL)
        """
    )
    abstract fun getInvitationsCountFlow(
        userId: UserId,
        types: Set<Long>,
        includeNullType: Boolean,
    ): Flow<Int>

    @Query(
        """
            DELETE FROM UserInvitationIdEntity
            WHERE
                user_id = :userId AND
                type IN (:types) OR (:includeNullType AND type IS NULL)
        """
    )
    abstract suspend fun deleteAll(
        userId: UserId,
        types: Set<Long>,
        includeNullType: Boolean,
    )

    @Query(
        """
            DELETE FROM UserInvitationIdEntity
            WHERE user_id = :userId 
            AND volume_id = :volumeId
            AND share_id = :shareId
            AND id = :invitationId
        """
    )
    abstract suspend fun deleteInvitation(
        userId: UserId,
        volumeId: String,
        shareId: String,
        invitationId: String,
    )
}
