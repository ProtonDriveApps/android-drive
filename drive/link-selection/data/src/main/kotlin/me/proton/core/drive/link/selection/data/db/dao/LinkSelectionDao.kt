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
package me.proton.core.drive.link.selection.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.drive.link.selection.data.db.entity.LinkSelectionEntity
import me.proton.core.drive.link.selection.domain.entity.SelectionId

@Dao
abstract class LinkSelectionDao : BaseDao<LinkSelectionEntity>() {

    @Query("SELECT EXISTS(SELECT * FROM LinkSelectionEntity WHERE selection_id = :selectionId)")
    abstract suspend fun hasSelectionId(selectionId: SelectionId): Boolean

    @Query("DELETE FROM LinkSelectionEntity WHERE selection_id = :selection_id")
    abstract suspend fun deleteAll(selection_id: SelectionId)

    @Query("SELECT * FROM LinkSelectionEntity WHERE selection_id = :selectionId")
    abstract suspend fun getAll(selectionId: SelectionId): List<LinkSelectionEntity>

    companion object {
        const val LINK_JOIN_STATEMENT = """
            LEFT JOIN LinkSelectionEntity ON
                LinkEntity.user_id = LinkSelectionEntity.user_id AND
                LinkEntity.share_id = LinkSelectionEntity.share_id AND
                LinkEntity.id = LinkSelectionEntity.link_id
        """
    }
}
