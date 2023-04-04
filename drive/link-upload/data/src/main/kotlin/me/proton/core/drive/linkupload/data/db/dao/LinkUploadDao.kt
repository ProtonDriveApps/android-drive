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
package me.proton.core.drive.linkupload.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.linkupload.data.db.entity.LinkUploadEntity
import me.proton.core.drive.linkupload.domain.entity.UploadState

@Dao
abstract class LinkUploadDao : BaseDao<LinkUploadEntity>() {

    @Insert
    abstract suspend fun insert(linkUploadEntity: LinkUploadEntity): Long

    @Insert
    abstract suspend fun insertAll(linkUploadEntities: List<LinkUploadEntity>): List<Long>

    @Query(
        "DELETE FROM LinkUploadEntity WHERE id = :id"
    )
    abstract suspend fun delete(id: Long)

    @Query(
        "SELECT * FROM LinkUploadEntity WHERE id = :id"
    )
    abstract suspend fun get(id: Long): LinkUploadEntity?

    @Query(
        "SELECT * FROM LinkUploadEntity WHERE id = :id"
    )
    abstract fun getFlow(id: Long): Flow<LinkUploadEntity?>

    @Query(
        "SELECT * FROM LinkUploadEntity WHERE user_id = :userId AND share_id = :shareId AND link_id = :linkId"
    )
    abstract suspend fun get(userId: UserId, shareId: String, linkId: String): LinkUploadEntity?

    @Query("""
        SELECT * FROM LinkUploadEntity WHERE user_id = :userId
    """)
    abstract fun getAllFlow(userId: UserId): Flow<List<LinkUploadEntity>>

    @Query("""
        SELECT * FROM LinkUploadEntity WHERE user_id = :userId AND parent_id = :parentLinkId
    """)
    abstract fun getAllFlow(userId: UserId, parentLinkId: String): Flow<List<LinkUploadEntity>>

    @Query("""
        UPDATE LinkUploadEntity SET state = :uploadState WHERE id = :id
    """)
    abstract fun updateUploadState(id: Long, uploadState: UploadState)

    @Query("""
        UPDATE LinkUploadEntity SET
            link_id = :linkId,
            revision_id = :revisionId,
            name = :name,
            node_key = :nodeKey,
            node_passphrase = :nodePassphrase,
            node_passphrase_signature = :nodePassphraseSignature,
            content_key_packet = :contentKeyPacket,
            content_key_packet_signature = :contentKeyPacketSignature
        WHERE id = :id
    """)
    abstract fun updateLinkIdAndRevisionId(
        id: Long,
        linkId: String,
        revisionId: String,
        name: String,
        nodeKey: String,
        nodePassphrase: String,
        nodePassphraseSignature: String,
        contentKeyPacket: String,
        contentKeyPacketSignature: String,
    )

    @Query("""
        UPDATE LinkUploadEntity SET manifest_signature = :manifestSignature WHERE id = :id
    """)
    abstract fun updateManifestSignature(id: Long, manifestSignature: String)

    @Query("""
        UPDATE LinkUploadEntity SET name = :name WHERE id = :id
    """)
    abstract fun updateName(id: Long, name: String)

    @Query("""
        UPDATE LinkUploadEntity SET uri = :uriString WHERE id = :id
    """)
    abstract fun updateUri(id: Long, uriString: String)

    @Query("""
        UPDATE LinkUploadEntity SET uri = :uriString, should_delete_source_uri = :shouldDeleteSourceUri WHERE id = :id
    """)
    abstract fun updateUri(id: Long, uriString: String, shouldDeleteSourceUri: Boolean)

    @Query("""
        UPDATE LinkUploadEntity SET size = :size WHERE id = :id
    """)
    abstract fun updateSize(id: Long, size: Long)

    @Query("""
        UPDATE LinkUploadEntity SET last_modified = :lastModified WHERE id = :id
    """)
    abstract fun updateLastModified(id: Long, lastModified: Long?)

    @Query("""
        UPDATE LinkUploadEntity SET
            media_resolution_width = :mediaResolutionWidth, media_resolution_height = :mediaResolutionHeight
        WHERE id = :id
    """)
    abstract fun updateMediaResolution(id: Long, mediaResolutionWidth: Long, mediaResolutionHeight: Long)
    @Query("""
        UPDATE LinkUploadEntity SET
            digests = :digests
        WHERE id = :id
    """)
    abstract fun updateDigests(id: Long, digests: String)

    open fun getDistinctFlow(id: Long) = getFlow(id).distinctUntilChanged()
}
