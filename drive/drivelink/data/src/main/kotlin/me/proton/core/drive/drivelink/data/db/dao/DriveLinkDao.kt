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

package me.proton.core.drive.drivelink.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.drivelink.data.db.entity.DriveLinkEntityWithBlock
import me.proton.core.drive.drivelink.data.db.entity.DriveLinkEntityWithBlock.Companion.BASE_PREFIX
import me.proton.core.drive.drivelink.data.db.entity.DriveLinkEntityWithBlock.Companion.DOWNLOAD_PREFIX
import me.proton.core.drive.drivelink.data.db.entity.DriveLinkEntityWithBlock.Companion.OFFLINE_PREFIX
import me.proton.core.drive.drivelink.data.db.entity.DriveLinkEntityWithBlock.Companion.SELECTION_PREFIX
import me.proton.core.drive.drivelink.data.db.entity.DriveLinkEntityWithBlock.Companion.TRASH_PREFIX
import me.proton.core.drive.link.data.db.LinkDao
import me.proton.core.drive.link.selection.data.db.dao.LinkSelectionDao
import me.proton.core.drive.linkdownload.data.db.LinkDownloadDao
import me.proton.core.drive.linkoffline.data.db.LinkOfflineDao
import me.proton.core.drive.linktrash.data.db.dao.LinkTrashDao

@Dao
interface DriveLinkDao : LinkDao {

    @Query(
        """
        SELECT $DRIVE_LINK_SELECT FROM $DRIVE_LINK_ENTITY 
        WHERE 
            LinkEntity.user_id = :userId AND 
            LinkEntity.share_id = :shareId AND
            LinkEntity.id = :linkId
            """
    )
    fun getLink(userId: UserId, shareId: String, linkId: String?): Flow<List<DriveLinkEntityWithBlock>>

    @Query(
        """
        SELECT $DRIVE_LINK_SELECT FROM $DRIVE_LINK_ENTITY 
        WHERE 
            LinkEntity.user_id = :userId AND 
            LinkEntity.share_id = :shareId AND
            LinkEntity.parent_id = :parentId AND
            ${LinkTrashDao.NOT_TRASHED_CONDITION}
            """
    )
    fun getLinks(userId: UserId, shareId: String, parentId: String?): Flow<List<DriveLinkEntityWithBlock>>

    @Query(
        """
        SELECT $DRIVE_LINK_SELECT FROM $DRIVE_LINK_ENTITY 
        WHERE 
            LinkEntity.user_id = :userId AND 
            LinkEntity.share_id = :shareId AND
            LinkEntity.id in (:ids)
            """
    )
    fun getLinks(userId: UserId, shareId: String, ids: List<String>): Flow<List<DriveLinkEntityWithBlock>>

    companion object {
        const val DRIVE_LINK_SELECT = """
            ShareEntity.${Column.VOLUME_ID} AS ${BASE_PREFIX}_${Column.VOLUME_ID},
            ShareEntity.${Column.USER_ID} AS ${BASE_PREFIX}_${Column.USER_ID},
            LinkEntity.*, 
            LinkFilePropertiesEntity.*, 
            LinkFolderPropertiesEntity.*, 
            LinkOfflineEntity.${Column.USER_ID} AS ${OFFLINE_PREFIX}_${Column.USER_ID},
            LinkOfflineEntity.${Column.SHARE_ID} AS ${OFFLINE_PREFIX}_${Column.SHARE_ID},
            LinkOfflineEntity.${Column.LINK_ID} AS ${OFFLINE_PREFIX}_${Column.LINK_ID},
            LinkDownloadStateEntity.${Column.USER_ID} AS ${DOWNLOAD_PREFIX}_${Column.USER_ID},
            LinkDownloadStateEntity.${Column.SHARE_ID} AS ${DOWNLOAD_PREFIX}_${Column.SHARE_ID},
            LinkDownloadStateEntity.${Column.LINK_ID} AS ${DOWNLOAD_PREFIX}_${Column.LINK_ID},
            LinkDownloadStateEntity.${Column.REVISION_ID} AS ${DOWNLOAD_PREFIX}_${Column.REVISION_ID},
            LinkDownloadStateEntity.${Column.STATE} AS ${DOWNLOAD_PREFIX}_${Column.STATE},
            LinkDownloadStateEntity.${Column.MANIFEST_SIGNATURE} AS ${DOWNLOAD_PREFIX}_${Column.MANIFEST_SIGNATURE},
            LinkDownloadStateEntity.${Column.SIGNATURE_ADDRESS} AS ${DOWNLOAD_PREFIX}_${Column.SIGNATURE_ADDRESS},
            DownloadBlockEntity.`${Column.INDEX}` AS ${DOWNLOAD_PREFIX}_${Column.INDEX},
            DownloadBlockEntity.${Column.URI} AS ${DOWNLOAD_PREFIX}_${Column.URI},
            DownloadBlockEntity.${Column.ENCRYPTED_SIGNATURE} AS ${DOWNLOAD_PREFIX}_${Column.ENCRYPTED_SIGNATURE},
            LinkTrashStateEntity.${Column.STATE} AS ${TRASH_PREFIX}_${Column.STATE},
            LinkSelectionEntity.${Column.SELECTION_ID} AS ${SELECTION_PREFIX}_${Column.SELECTION_ID}
        """

        const val DRIVE_LINK_LINK_ENTITY_JOIN_STATEMENT = """
            ${LinkDao.PROPERTIES_ENTITIES_JOIN_STATEMENT}
            ${LinkOfflineDao.LINK_JOIN_STATEMENT}
            ${LinkDownloadDao.LINK_JOIN_STATEMENT}
            ${LinkTrashDao.LINK_JOIN_STATEMENT}
            ${LinkSelectionDao.LINK_JOIN_STATEMENT}
            LEFT JOIN ShareEntity ON
                LinkEntity.${Column.SHARE_ID} = ShareEntity.${Column.ID} AND
                LinkEntity.${Column.USER_ID} = ShareEntity.${Column.USER_ID}
        """

        const val DRIVE_LINK_ENTITY = """
            LinkEntity $DRIVE_LINK_LINK_ENTITY_JOIN_STATEMENT
        """
    }
}
