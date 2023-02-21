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
package me.proton.core.drive.linkoffline.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.data.db.LinkDao
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId

@Dao
interface LinkOfflineDao : LinkDao {

    @Query(
        """
        SELECT EXISTS(SELECT * FROM LinkOfflineEntity
            WHERE user_id = :userId AND share_id = :shareId AND link_id = :linkId)
    """
    )
    suspend fun hasLinkOfflineEntity(userId: UserId, shareId: String, linkId: String): Boolean

    @Query("""
        SELECT EXISTS (SELECT * FROM LinkOfflineEntity
            WHERE user_id = :userId AND share_id = :shareId AND link_id IN (:linkIds))
    """)
    suspend fun hasAnyLinkOfflineEntity(userId: UserId, shareId: String, linkIds: List<String>): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(vararg linkOfflineEntities: LinkOfflineEntity)

    @Delete
    suspend fun delete(vararg linkOfflineEntities: LinkOfflineEntity)

    @Transaction
    suspend fun hasAnyLinkOfflineEntity(linkIds: Set<LinkId>): Boolean =
        linkIds
            .groupBy { linkId -> linkId.userId }
            .any { (userId, list) ->
                list
                    .groupBy { linkId -> linkId.shareId }
                    .any { (shareId, list) ->
                        hasAnyLinkOfflineEntity(userId, shareId.id, list.map { linkId -> linkId.id })
                    }
                }

    companion object {
        const val LINK_JOIN_STATEMENT = """
            LEFT JOIN LinkOfflineEntity ON
                LinkEntity.user_id = LinkOfflineEntity.user_id AND
                LinkEntity.share_id = LinkOfflineEntity.share_id AND
                LinkEntity.id = LinkOfflineEntity.link_id
        """

        const val OFFLINE_CONDITION = """
            (LinkOfflineEntity.link_id IS NOT NULL)
        """
    }
}
