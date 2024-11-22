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
package me.proton.core.drive.drivelink.selection.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.drivelink.data.db.dao.DriveLinkDao
import me.proton.core.drive.drivelink.data.db.entity.DriveLinkEntity
import me.proton.core.drive.drivelink.data.db.entity.DriveLinkEntity.Companion.SELECTION_PREFIX
import me.proton.core.drive.link.selection.data.db.dao.LinkSelectionDao
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.linktrash.data.db.dao.LinkTrashDao

@Dao
interface DriveLinkSelectionDao : DriveLinkDao {

    @Query(
        """
        SELECT ${DriveLinkDao.DRIVE_LINK_SELECT},
            LinkSelectionEntity.${Column.SELECTION_ID} AS ${SELECTION_PREFIX}_${Column.SELECTION_ID}
        FROM ${DriveLinkDao.DRIVE_LINK_ENTITY}
          ${LinkSelectionDao.LINK_JOIN_STATEMENT}
        WHERE ${SELECTION_PREFIX}_${Column.SELECTION_ID} = :selectionId AND ${LinkTrashDao.NOT_TRASHED_CONDITION}
        """
    )
    fun getSelectedLinks(selectionId: SelectionId): Flow<List<DriveLinkEntity>>
}
