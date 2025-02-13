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
package me.proton.core.drive.linknode.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.data.db.LinkDao
import me.proton.core.drive.link.data.db.LinkDao.Companion.PROPERTIES_ENTITIES_JOIN_STATEMENT
import me.proton.core.drive.link.data.db.entity.LinkWithProperties
import me.proton.core.drive.link.data.db.entity.LinkWithPropertiesEntity
import me.proton.core.drive.link.data.extension.toLinkWithProperties

@OptIn(ExperimentalCoroutinesApi::class)
@Dao
interface LinkAncestorDao : LinkDao {
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
            WITH RECURSIVE ancestors(link_id, share_id, user_id, rank) AS (
                SELECT :linkId, :shareId, :userId, 0
                UNION ALL SELECT parent_id, LinkEntity.share_id, LinkEntity.user_id, rank+1 FROM LinkEntity, ancestors
                WHERE 
                    LinkEntity.id = ancestors.link_id AND
                    LinkEntity.share_id = ancestors.share_id AND
                    LinkEntity.user_id = ancestors.user_id
            )
            SELECT LinkEntity.*, LinkFilePropertiesEntity.*, LinkFolderPropertiesEntity.*, LinkAlbumPropertiesEntity.*
            FROM ancestors INNER JOIN LinkEntity ON
                ancestors.link_id = LinkEntity.id AND ancestors.share_id = LinkEntity.share_id AND
                ancestors.user_id = LinkEntity.user_id
            $PROPERTIES_ENTITIES_JOIN_STATEMENT 
            ORDER BY rank DESC
        """
    )
    fun getAncestors(userId: UserId, shareId: String, linkId: String): Flow<List<LinkWithPropertiesEntity>>

    fun getLinkWithPropertiesAncestors(
        userId: UserId,
        shareId: String,
        linkId: String
    ): Flow<List<LinkWithProperties>> =
        getAncestors(userId, shareId, linkId)
            .distinctUntilChanged()
            .mapLatest { list ->
                list.map { linkWithPropertiesEntity -> linkWithPropertiesEntity.toLinkWithProperties() }
            }
}
