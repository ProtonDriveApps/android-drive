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
package me.proton.core.drive.link.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.data.db.entity.LinkAlbumPropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkEntity
import me.proton.core.drive.link.data.db.entity.LinkFilePropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkFolderPropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkTagEntity
import me.proton.core.drive.link.data.db.entity.LinkWithProperties
import me.proton.core.drive.link.data.db.entity.LinkWithPropertiesEntity
import me.proton.core.drive.link.data.extension.toLinkWithProperties
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId

@OptIn(ExperimentalCoroutinesApi::class)
@Dao
interface LinkDao {

    @Query("""
        SELECT * FROM $LINK_WITH_PROPERTIES_ENTITY WHERE
            LinkEntity.user_id = :userId AND LinkEntity.share_id = :shareId AND id = :linkId
    """)
    fun getFlow(userId: UserId, shareId: String, linkId: String): Flow<LinkWithPropertiesEntity?>

    @Query("""
        SELECT * FROM $LINK_WITH_PROPERTIES_ENTITY WHERE
            LinkEntity.user_id = :userId AND LinkEntity.share_id = :shareId AND id IN(:ids)
    """)
    fun getFlow(userId: UserId, shareId: String, ids: List<String>): Flow<List<LinkWithPropertiesEntity>>

    @Query(
        "SELECT EXISTS(SELECT * FROM LinkEntity WHERE user_id = :userId AND share_id = :shareId AND id = :linkId)"
    )
    fun hasLinkEntity(userId: UserId, shareId: String, linkId: String): Flow<Boolean>

    @Query(
        "SELECT EXISTS(SELECT * FROM LinkFilePropertiesEntity WHERE file_user_id = :userId AND file_share_id = :shareId)"
    )
    suspend fun hasAnyFileEntity(userId: UserId, shareId: String): Boolean

    @Query(
        """
            SELECT LinkEntity.*, LinkFilePropertiesEntity.*, LinkFolderPropertiesEntity.*, LinkAlbumPropertiesEntity.*
            FROM $LINK_WITH_PROPERTIES_ENTITY 
                JOIN ShareEntity 
                ON LinkEntity.user_id = ShareEntity.user_id AND
                    LinkEntity.share_id = ShareEntity.id
            WHERE ShareEntity.user_id = :userId AND 
                ShareEntity.volume_id = :volumeId AND
                LinkEntity.id = :linkId 
        """
    )
    suspend fun getLinks(userId: UserId, volumeId: String, linkId: String): List<LinkWithPropertiesEntity>

    @Update
    suspend fun update(vararg linkEntities: LinkEntity): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(vararg linkEntities: LinkEntity)

    @Transaction
    suspend fun insertOrUpdate(vararg linkEntities: LinkEntity) {
        update(*linkEntities)
        insertOrIgnore(*linkEntities)
    }

    @Query("DELETE FROM LinkEntity WHERE user_id = :userId AND share_id = :shareId AND id in (:linkIds)")
    suspend fun delete(userId: UserId, shareId: String, linkIds: List<String>)

    @Delete
    suspend fun delete(vararg linkEntities: LinkEntity)

    @Update
    suspend fun update(vararg linkFilePropertiesEntities: LinkFilePropertiesEntity): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(vararg linkFilePropertiesEntities: LinkFilePropertiesEntity)

    @Transaction
    suspend fun insertOrUpdate(vararg linkFilePropertiesEntities: LinkFilePropertiesEntity) {
        update(*linkFilePropertiesEntities)
        insertOrIgnore(*linkFilePropertiesEntities)
    }

    @Update
    suspend fun update(vararg linkFolderPropertiesEntities: LinkFolderPropertiesEntity): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(vararg linkFolderPropertiesEntities: LinkFolderPropertiesEntity)

    @Transaction
    suspend fun insertOrUpdate(vararg linkFolderPropertiesEntities: LinkFolderPropertiesEntity) {
        update(*linkFolderPropertiesEntities)
        insertOrIgnore(*linkFolderPropertiesEntities)
    }

    @Update
    suspend fun update(vararg linkAlbumPropertiesEntities: LinkAlbumPropertiesEntity): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(vararg linkAlbumPropertiesEntities: LinkAlbumPropertiesEntity)

    @Transaction
    suspend fun insertOrUpdate(vararg linkAlbumPropertiesEntities: LinkAlbumPropertiesEntity) {
        update(*linkAlbumPropertiesEntities)
        insertOrIgnore(*linkAlbumPropertiesEntities)
    }

    fun getLinkWithPropertiesFlow(userId: UserId, shareId: String, linkId: String): Flow<LinkWithProperties?> =
        getFlow(userId, shareId, linkId)
            .distinctUntilChanged()
            .mapLatest { linkWithPropertiesEntity ->
                linkWithPropertiesEntity?.toLinkWithProperties()
            }

    fun getLinksWithPropertiesFlow(userId: UserId, shareId: String, ids: List<String>): Flow<List<LinkWithProperties>> =
        getFlow(userId, shareId, ids)
            .distinctUntilChanged()
            .mapLatest { links ->
                links.map { linkWithPropertiesEntity -> linkWithPropertiesEntity.toLinkWithProperties() }
            }

    @Transaction
    suspend fun insertOrUpdate(vararg linksWithProperties: LinkWithProperties) {
        insertOrUpdate(
            *linksWithProperties.map { linkEntities -> linkEntities.link }.toTypedArray()
        )
        insertOrUpdate(
            *linksWithProperties.mapNotNull { linkFileProperties ->
                linkFileProperties.properties as? LinkFilePropertiesEntity
            }.toTypedArray()
        )
        insertOrUpdate(
            *linksWithProperties.mapNotNull { linkFolderProperties ->
                linkFolderProperties.properties as? LinkFolderPropertiesEntity
            }.toTypedArray()
        )
        insertOrUpdate(
            *linksWithProperties.mapNotNull { linkAlbumProperties ->
                linkAlbumProperties.properties as? LinkAlbumPropertiesEntity
            }.toTypedArray()
        )
    }

    suspend fun insertTags(linkId: LinkId, tags: List<Long>) {
        insertTags(
            *tags.map { tag -> LinkTagEntity(linkId.userId, linkId.shareId.id, linkId.id, tag) }.toTypedArray()
        )
    }

    suspend fun deleteTags(linkId: LinkId, tags: List<Long>) {
        deleteTags(
            *tags.map { tag -> LinkTagEntity(linkId.userId, linkId.shareId.id, linkId.id, tag) }.toTypedArray()
        )
    }
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(vararg entity: LinkTagEntity)
    @Delete
    suspend fun deleteTags(vararg entity: LinkTagEntity)

    suspend fun delete(vararg linksWithProperties: LinkWithProperties) =
        delete(
            *linksWithProperties.map { linkWithProperties -> linkWithProperties.link }.toTypedArray()
        )

    companion object {
        const val PROPERTIES_ENTITIES_JOIN_STATEMENT = """
            LEFT JOIN LinkFilePropertiesEntity ON
                LinkEntity.share_id = LinkFilePropertiesEntity.file_share_id AND
                LinkEntity.id = LinkFilePropertiesEntity.file_link_id
            LEFT JOIN LinkFolderPropertiesEntity ON
                LinkEntity.share_id = LinkFolderPropertiesEntity.folder_share_id AND
                LinkEntity.id = LinkFolderPropertiesEntity.folder_link_id
            LEFT JOIN LinkAlbumPropertiesEntity ON
                LinkEntity.share_id = LinkAlbumPropertiesEntity.album_share_id AND
                LinkEntity.id = LinkAlbumPropertiesEntity.album_link_id
            LEFT JOIN (SELECT tag_share_id, tag_link_id, GROUP_CONCAT(tag, ',') AS tags_data FROM LinkTagEntity) LinkTagsData ON
                LinkEntity.share_id = LinkTagsData.tag_share_id AND
                LinkEntity.id = LinkTagsData.tag_link_id
        """
        const val LINK_WITH_PROPERTIES_ENTITY = "LinkEntity $PROPERTIES_ENTITIES_JOIN_STATEMENT"
    }
}
