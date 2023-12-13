/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.shareurl.base.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.shareurl.base.data.db.entity.ShareUrlEntity

@Dao
abstract class ShareUrlDao : BaseDao<ShareUrlEntity>() {

    @Query("SELECT EXISTS($QUERY_GET_SHARE_URL)")
    abstract suspend fun hasShareUrlEntity(userId: UserId, shareUrlId: String): Boolean

    @Query("SELECT EXISTS($QUERY_GET_SHARE_URL_FOR_LINK)")
    abstract suspend fun hasShareUrlEntityForLink(linkId: String): Boolean

    @Query(
        "SELECT EXISTS(SELECT * FROM ShareUrlEntity WHERE user_id = :userId AND volume_id = :volumeId)"
    )
    abstract suspend fun hasShareUrlEntities(userId: UserId, volumeId: String): Boolean

    @Query(QUERY_GET_SHARE_URL)
    abstract fun getFlow(userId: UserId, shareUrlId: String): Flow<ShareUrlEntity?>

    @Query(QUERY_GET_SHARE_URL_FOR_LINK)
    abstract fun getFlowForLink(linkId: String): Flow<ShareUrlEntity?>

    @Query(QUERY_GET_SHARE_URL)
    abstract suspend fun get(userId: UserId, shareUrlId: String): ShareUrlEntity?

    @Transaction
    @Query(QUERY_GET_ALL_SHARE_URLS)
    abstract fun getAllFlow(userId: UserId, volumeId: String): Flow<List<ShareUrlEntity>>

    @Transaction
    @Query(QUERY_GET_ALL_SHARE_URLS)
    abstract suspend fun getAll(userId: UserId, volumeId: String): List<ShareUrlEntity>

    @Transaction
    @Query(
        """
        DELETE FROM ShareUrlEntity WHERE id IN (
                SELECT ShareUrlEntity.id FROM ShareUrlEntity 
                    LEFT JOIN ShareEntity ON ShareEntity.id = ShareUrlEntity.share_id
                    LEFT JOIN LinkEntity ON LinkEntity.id = ShareEntity.link_id
                WHERE LinkEntity.user_id = :userId AND LinkEntity.share_id = :shareId
        )
                """
    )
    abstract suspend fun deleteAllForLinksInShare(userId: UserId, shareId: String)

    @Query("""DELETE FROM ShareUrlEntity WHERE user_id = :userId AND volume_id = :volumeId""")
    abstract suspend fun deleteAll(userId: UserId, volumeId: String)


    @Query("""DELETE FROM ShareUrlEntity WHERE id = :id""")
    abstract suspend fun delete(id: String)

    companion object {
        private const val QUERY_GET_SHARE_URL =
            """
            SELECT * FROM ShareUrlEntity WHERE user_id = :userId AND id = :shareUrlId
            """

        private const val QUERY_GET_SHARE_URL_FOR_LINK =
            """
            SELECT ShareUrlEntity.* FROM ShareUrlEntity 
                    LEFT JOIN ShareEntity 
                        ON ShareUrlEntity.share_id = ShareEntity.id
                    WHERE ShareEntity.link_id = :linkId
            """

        private const val QUERY_GET_ALL_SHARE_URLS =
            """
                SELECT ShareUrlEntity.* FROM ShareUrlEntity 
                    LEFT JOIN ShareEntity ON ShareEntity.id = ShareUrlEntity.share_id
                    LEFT JOIN LinkEntity ON LinkEntity.id = ShareEntity.link_id
                WHERE LinkEntity.user_id = :userId AND ShareUrlEntity.volume_id = :volumeId
            """
    }
}
