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
package me.proton.core.drive.folder.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.data.db.LinkDao
import me.proton.core.drive.link.data.db.LinkDao.Companion.LINK_WITH_PROPERTIES_ENTITY
import me.proton.core.drive.link.data.db.entity.LinkWithProperties
import me.proton.core.drive.link.data.db.entity.LinkWithPropertiesEntity
import me.proton.core.drive.link.data.extension.toLinkWithProperties

@OptIn(ExperimentalCoroutinesApi::class)
@Dao
interface FolderDao : LinkDao {

    @Query("""
        SELECT EXISTS(
            SELECT * FROM LinkEntity WHERE user_id = :userId AND share_id = :shareId AND parent_id = :folderId
        )
    """)
    suspend fun hasFolderChildren(userId: UserId, shareId: String, folderId: String): Boolean

    @Query("""
        SELECT EXISTS(
            SELECT * FROM LinkEntity WHERE
                user_id = :userId AND share_id = :shareId AND parent_id = :folderId AND hash = :hash
        )
    """)
    suspend fun hasFolderChildrenWithHash(userId: UserId, shareId: String, folderId: String, hash: String): Boolean

    @Query("""
        SELECT * FROM $LINK_WITH_PROPERTIES_ENTITY WHERE
            user_id = :userId AND 
            share_id = :shareId AND 
            parent_id = :folderLinkId
    """)
    fun getFolderChildren(
        userId: UserId,
        shareId: String,
        folderLinkId: String
    ): Flow<List<LinkWithPropertiesEntity>>

    fun getFolderChildrenFlow(userId: UserId, shareId: String, folderLinkId: String): Flow<List<LinkWithProperties>> =
        getFolderChildren(userId, shareId, folderLinkId)
            .distinctUntilChanged()
            .mapLatest { links ->
                links.map { linkWithPropertiesEntity ->
                    linkWithPropertiesEntity.toLinkWithProperties()
                }
            }
}
