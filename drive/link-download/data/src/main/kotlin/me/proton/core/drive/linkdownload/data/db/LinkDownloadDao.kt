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
package me.proton.core.drive.linkdownload.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.link.data.db.LinkDao
import me.proton.core.drive.linkdownload.data.db.entity.DownloadBlockEntity
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadState
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadStateEntity
import me.proton.core.drive.linkdownload.data.extension.toDownloadBlockEntity
import me.proton.core.drive.linkdownload.domain.entity.DownloadState

@Dao
interface LinkDownloadDao : LinkDao {

    @Transaction
    @Query("""
        SELECT EXISTS(
            SELECT * FROM LinkDownloadStateEntity WHERE
                user_id = :userId AND share_id = :shareId AND link_id = :linkId AND revision_id = :revisionId
        )
    """)
    suspend fun hasLinkDownloadStateEntity(userId: UserId, shareId: String, linkId: String, revisionId: String): Boolean

    @Update
    suspend fun update(vararg linkDownloadStateEntities: LinkDownloadStateEntity): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(vararg linkDownloadStateEntities: LinkDownloadStateEntity)

    @Transaction
    suspend fun insertOrUpdate(vararg linkDownloadStateEntities: LinkDownloadStateEntity) {
        update(*linkDownloadStateEntities)
        insertOrIgnore(*linkDownloadStateEntities)
    }

    @Delete
    suspend fun delete(vararg linkDownloadStateEntities: LinkDownloadStateEntity)

    @Query("""
        DELETE FROM LinkDownloadStateEntity WHERE
            user_id = :userId AND share_id = :shareId AND link_id = :linkId AND revision_id = :revisionId
    """)
    suspend fun delete(userId: UserId, shareId: String, linkId: String, revisionId: String)

    @Query("""
        SELECT * FROM LinkDownloadStateEntity WHERE
            user_id = :userId AND share_id = :shareId AND link_id = :linkId AND revision_id = :revisionId
    """)
    fun getLinkDownloadStateFlow(userId: UserId, shareId: String, linkId: String, revisionId: String): Flow<LinkDownloadStateEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(vararg downloadBlockEntities: DownloadBlockEntity)

    @Query("""
        SELECT * FROM DownloadBlockEntity WHERE
            user_id = :userId AND 
            share_id = :shareId AND 
            link_id = :linkId AND 
            revision_id = :revisionId
            ORDER BY `index` ASC LIMIT :limit OFFSET :offset
    """)
    suspend fun getDownloadBlocks(
        userId: UserId,
        shareId: String,
        linkId: String,
        revisionId: String,
        limit: Int,
        offset: Int
    ): List<DownloadBlockEntity>

    @Query("""
        SELECT COUNT(*) FROM DownloadBlockEntity WHERE
            user_id = :userId AND 
            share_id = :shareId AND 
            link_id = :linkId AND 
            revision_id = :revisionId
    """)
    fun getDownloadBlocksCountFlow(
        userId: UserId,
        shareId: String,
        linkId: String,
        revisionId: String,
    ): Flow<Int>

    @Query("""
        DELETE FROM DownloadBlockEntity WHERE
            user_id = :userId AND share_id = :shareId AND link_id = :linkId AND revision_id = :revisionId
    """)
    suspend fun deleteDownloadBlocks(userId: UserId, shareId: String, linkId: String, revisionId: String)

    // We cannot use "SELECT * FROM" here because otherwise room throws an exception
    // java.lang.NullPointerException: Parameter specified as non-null is null: [...] parameter shareId
    @Query("""
        SELECT * FROM LinkDownloadStateEntity
        WHERE
            LinkDownloadStateEntity.user_id = :userId AND 
            LinkDownloadStateEntity.share_id = :shareId AND 
            LinkDownloadStateEntity.link_id = :linkId AND
            LinkDownloadStateEntity.revision_id = :revisionId
    """)
    fun getDownloadStateFlow(
        userId: UserId,
        shareId: String,
        linkId: String,
        revisionId: String,
    ): Flow<LinkDownloadStateEntity?>

    @Query("""
        SELECT 
            LinkDownloadStateEntity.state
        FROM 
            LinkEntity 
        LEFT JOIN LinkDownloadStateEntity ON 
            LinkDownloadStateEntity.user_id = LinkEntity.user_id AND 
            LinkDownloadStateEntity.share_id = LinkEntity.share_id AND 
            LinkDownloadStateEntity.link_id = LinkEntity.id
        WHERE
            LinkEntity.user_id = :userId AND 
            LinkEntity.share_id = :shareId AND 
            LinkEntity.parent_id = :folderId AND
            LinkEntity.mime_type NOT IN (:excludeMimeTypes)
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getAllChildrenStates(
        userId: UserId,
        shareId: String,
        folderId: String,
        excludeMimeTypes: Set<String>,
        limit: Int,
        offset: Int,
    ): List<LinkDownloadState?>

    @Query("""
        SELECT
            LinkDownloadStateEntity.state
        FROM
            LinkEntity
        LEFT JOIN LinkDownloadStateEntity ON
            LinkDownloadStateEntity.user_id = LinkEntity.user_id AND
            LinkDownloadStateEntity.link_id = LinkEntity.id
        INNER JOIN AlbumPhotoListingEntity ON
            AlbumPhotoListingEntity.user_id = LinkEntity.user_id AND
            AlbumPhotoListingEntity.id = LinkEntity.id AND
            AlbumPhotoListingEntity.album_id = :albumId
        WHERE
            LinkEntity.user_id = :userId
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getAllAlbumChildrenStates(
        userId: UserId,
        albumId: String,
        limit: Int,
        offset: Int,
    ): List<LinkDownloadState?>

    @Transaction
    suspend fun insertOrUpdate(
        userId: UserId,
        shareId: String,
        linkId: String,
        revisionId: String,
        downloadState: DownloadState,
        blocks: List<Block>?,
    ) {
        deleteDownloadBlocks(userId, shareId, linkId, revisionId)
        insertOrUpdate(
            LinkDownloadStateEntity(
                userId = userId,
                shareId = shareId,
                linkId = linkId,
                revisionId = revisionId,
                state = when (downloadState) {
                    is DownloadState.Downloading -> LinkDownloadState.DOWNLOADING
                    is DownloadState.Error -> LinkDownloadState.ERROR
                    is DownloadState.Downloaded -> LinkDownloadState.DOWNLOADED
                },
                manifestSignature = (downloadState as? DownloadState.Downloaded)?.manifestSignature,
                signatureAddress = (downloadState as? DownloadState.Downloaded)?.signatureAddress,
            )
        )
        if (downloadState is DownloadState.Downloaded && blocks != null) {
            insertOrIgnore(
                *blocks.map { block ->
                    block.toDownloadBlockEntity(userId, shareId, linkId, revisionId)
                }.toTypedArray()
            )
        }
    }

    companion object {
        const val LINK_JOIN_STATEMENT = """
            LEFT JOIN  LinkDownloadStateEntity ON
                LinkEntity.user_id = LinkDownloadStateEntity.user_id AND
                LinkEntity.share_id = LinkDownloadStateEntity.share_id AND
                LinkEntity.id = LinkDownloadStateEntity.link_id AND
                (LinkFilePropertiesEntity.revision_id = LinkDownloadStateEntity.revision_id OR
                LinkFolderPropertiesEntity.folder_link_id IS NOT NULL OR
                LinkAlbumPropertiesEntity.album_link_id IS NOT NULL
                )
        """
    }
}
