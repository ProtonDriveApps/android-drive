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
package me.proton.core.drive.share.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class ShareDao : BaseDao<ShareEntity>() {

    @Transaction
    @Query(
        "SELECT EXISTS(SELECT * FROM ShareEntity WHERE user_id = :userId AND id = :shareId)"
    )
    abstract suspend fun hasShareEntity(userId: UserId, shareId: String): Boolean

    @Transaction
    @Query(
        """
            SELECT EXISTS(
                SELECT * FROM ShareEntity WHERE
                    user_id = :userId AND
                    id = :shareId AND
                    NULLIF(`key`, '') IS NOT NULL AND
                    NULLIF(`passphrase`, '') IS NOT NULL AND
                    NULLIF(`passphrase_signature`, '') IS NOT NULL AND
                    address_id IS NOT NULL
            )
        """
    )
    abstract suspend fun hasShareEntityWithKey(userId: UserId, shareId: String): Boolean

    @Transaction
    @Query(
        "SELECT EXISTS(SELECT * FROM ShareEntity WHERE user_id = :userId AND type = :type)"
    )
    abstract suspend fun hasShareEntities(userId: UserId, type: Long): Boolean

    @Transaction
    @Query(
        "SELECT EXISTS(SELECT * FROM ShareEntity WHERE user_id = :userId AND volume_id = :volumeId AND type = :type)"
    )
    abstract suspend fun hasShareEntities(userId: UserId, volumeId: String, type: Long): Boolean

    @Query(
        QUERY_GET_SHARE
    )
    abstract fun getFlow(userId: UserId, shareId: String): Flow<ShareEntity?>

    @Query(
        QUERY_GET_SHARE
    )
    abstract suspend fun get(userId: UserId, shareId: String): ShareEntity?

    @Transaction
    @Query(
        QUERY_GET_ALL_SHARES
    )
    abstract fun getAllFlow(userId: UserId): Flow<List<ShareEntity>>

    @Transaction
    @Query("""
        $QUERY_GET_ALL_SHARES AND volume_id = :volumeId
    """)
    abstract fun getAllFlow(userId: UserId, volumeId: String): Flow<List<ShareEntity>>

    @Transaction
    @Query(
        QUERY_GET_ALL_SHARES
    )
    abstract suspend fun getAll(userId: UserId): List<ShareEntity>

    @Query("""DELETE FROM ShareEntity WHERE user_id = :userId AND id = :shareId""")
    abstract suspend fun delete(userId: UserId, shareId: String)

    @Query("""DELETE FROM ShareEntity WHERE user_id = :userId AND id IN (:shareIds)""")
    abstract suspend fun deleteAll(userId: UserId, shareIds: List<String>)

    open fun getDistinctFlow(userId: UserId, id: String) = getFlow(userId, id).distinctUntilChanged()

    companion object {
        private const val QUERY_GET_SHARE =
            """
                SELECT * FROM ShareEntity WHERE user_id = :userId AND id = :shareId
            """

        private const val QUERY_GET_ALL_SHARES =
            """
                SELECT * FROM ShareEntity WHERE user_id = :userId
            """
    }
}
