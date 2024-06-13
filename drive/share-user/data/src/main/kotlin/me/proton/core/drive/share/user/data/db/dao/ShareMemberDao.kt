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
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.share.user.data.db.entity.ShareMemberEntity

@Dao
abstract class ShareMemberDao : BaseDao<ShareMemberEntity>() {

    @Query(
        """SELECT EXISTS(
            SELECT * FROM ShareMemberEntity
            WHERE user_id = :userId AND share_id = :shareId
        )"""
    )
    abstract suspend fun hasMembers(userId: UserId, shareId: String): Boolean

    @Query(
        """
            SELECT * FROM ShareMemberEntity
            WHERE user_id = :userId AND share_id = :shareId
            LIMIT :limit
        """
    )
    abstract fun getMembersFlow(
        userId: UserId,
        shareId: String,
        limit: Int,
    ): Flow<List<ShareMemberEntity>>

    @Query(
        """
            DELETE FROM ShareMemberEntity
            WHERE user_id = :userId AND share_id = :shareId
        """
    )
    abstract suspend fun deleteAll(userId: UserId, shareId: String)

    @Query(
        """
            SELECT * FROM ShareMemberEntity 
            WHERE user_id = :userId AND share_id = :shareId AND id = :memberId
        """
    )
    abstract fun getMemberFlow(
        userId: UserId,
        shareId: String,
        memberId: String,
    ): Flow<ShareMemberEntity?>


    @Query(
        """
            UPDATE ShareMemberEntity SET permissions = :permissions
            WHERE user_id = :userId AND share_id = :shareId AND id = :memberId
        """
    )
    abstract suspend fun updatePermission(
        userId: UserId,
        shareId: String,
        memberId: String,
        permissions: Long,
    )

    @Query(
        """
            DELETE FROM ShareMemberEntity
            WHERE user_id = :userId AND share_id = :shareId AND id = :memberId
        """
    )
    abstract suspend fun deleteMember(
        userId: UserId,
        shareId: String,
        memberId: String,
    )
}
