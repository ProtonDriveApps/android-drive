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

package me.proton.core.drive.drivelink.shared.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.data.db.dao.DriveLinkDao
import me.proton.core.drive.drivelink.data.db.entity.DriveLinkEntityWithBlock
import me.proton.core.drive.link.data.db.LinkDao
import me.proton.core.drive.linkdownload.data.db.LinkDownloadDao
import me.proton.core.drive.linkoffline.data.db.LinkOfflineDao
import me.proton.core.drive.linktrash.data.db.dao.LinkTrashDao
import me.proton.core.drive.share.data.db.ShareMembershipDao

@Dao
interface DriveLinkSharedDao : DriveLinkDao {

    @Transaction
    @Query(
        """
        SELECT ${DriveLinkDao.DRIVE_LINK_SELECT} FROM 
            ShareUrlEntity
            LEFT JOIN ShareEntity on ShareEntity.user_id = ShareUrlEntity.user_id AND ShareEntity.id = ShareUrlEntity.share_id
            LEFT JOIN LinkEntity on LinkEntity.user_id = ShareEntity.user_id AND LinkEntity.id = ShareEntity.link_id
            ${LinkDao.PROPERTIES_ENTITIES_JOIN_STATEMENT}
            ${LinkOfflineDao.LINK_JOIN_STATEMENT}
            ${LinkDownloadDao.LINK_JOIN_STATEMENT}
            ${LinkTrashDao.LINK_JOIN_STATEMENT}
            ${ShareMembershipDao.LINK_JOIN_STATEMENT}
        WHERE 
            LinkEntity.user_id = :userId AND 
            ShareUrlEntity.volume_id = :volumeId AND
            ${LinkTrashDao.NOT_TRASHED_CONDITION}
            """
    )
    fun getSharedLinks(userId: UserId, volumeId: String): Flow<List<DriveLinkEntityWithBlock>>
}
