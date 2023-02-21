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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.data.db.LinkDao
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linktrash.data.db.entity.LinkTrashStateEntity

@Dao
interface LinkTrashDao : LinkDao {
    @Update
    suspend fun update(vararg linkTrashStateEntities: LinkTrashStateEntity): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(vararg linkTrashStateEntities: LinkTrashStateEntity)

    @Transaction
    suspend fun insertOrUpdate(vararg linkTrashStateEntities: LinkTrashStateEntity) {
        update(*linkTrashStateEntities)
        insertOrIgnore(*linkTrashStateEntities)
    }

    @Delete
    suspend fun delete(vararg linkTrashStateEntities: LinkTrashStateEntity)

    @Query(
        """
        DELETE FROM LinkTrashStateEntity WHERE user_id = :userId AND share_id = :shareId AND link_id IN (:linkIds)
    """
    )
    override suspend fun delete(userId: UserId, shareId: String, linkIds: List<String>)

    @Query("""UPDATE LinkTrashStateEntity SET state = "DELETED" WHERE user_id = :userId AND share_id = :shareId""")
    suspend fun delete(userId: UserId, shareId: String)

    @Query("SELECT EXISTS(SELECT * FROM LinkTrashStateEntity WHERE user_id = :userId AND share_id = :shareId AND $TRASHED_CONDITION)")
    fun hasTrashContent(userId: UserId, shareId: String): Flow<Boolean>

    @Query("""
        SELECT EXISTS(SELECT * FROM LinkTrashStateEntity
            WHERE user_id = :userId AND share_id = :shareId AND link_id = :linkId AND $TRASHED_CONDITION)
    """)
    suspend fun isTrashed(userId: UserId, shareId: String, linkId: String): Boolean

    @Query("""
        SELECT EXISTS (SELECT * FROM LinkTrashStateEntity
            WHERE user_id = :userId AND share_id = :shareId AND link_id IN (:linkIds) AND $TRASHED_CONDITION)
    """)
    suspend fun isAnyTrashed(userId: UserId, shareId: String, linkIds: List<String>): Boolean

    @Transaction
    suspend fun isAnyTrashed(linkIds: Set<LinkId>): Boolean =
        linkIds
            .groupBy { linkId -> linkId.userId }
            .any { (userId, list) ->
                list
                    .groupBy { linkId -> linkId.shareId }
                    .any { (shareId, list) ->
                        isAnyTrashed(userId, shareId.id, list.map { linkId -> linkId.id })
                    }
            }

    companion object {
        const val LINK_JOIN_STATEMENT = """
            LEFT JOIN LinkTrashStateEntity ON
                LinkEntity.user_id = LinkTrashStateEntity.user_id AND
                LinkEntity.share_id = LinkTrashStateEntity.share_id AND
                LinkEntity.id = LinkTrashStateEntity.link_id
        """

        const val NOT_TRASHED_CONDITION = """
            (LinkTrashStateEntity.state IS NULL OR LinkTrashStateEntity.state == "TRASHING")
        """

        const val TRASHED_CONDITION = """
            (
                LinkTrashStateEntity.state IS NOT NULL AND 
                LinkTrashStateEntity.state != "TRASHING" AND 
                LinkTrashStateEntity.state != "DELETED"
            )
        """
    }
}
