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
package me.proton.core.drive.linkupload.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import me.proton.core.drive.linkupload.data.db.entity.UploadBulkEntity
import me.proton.core.drive.linkupload.data.db.entity.UploadBulkUriStringEntity
import me.proton.core.drive.linkupload.data.db.entity.UploadBulkWithUri

@Dao
interface UploadBulkDao {
    @Insert
    suspend fun insert(uploadBulkEntity: UploadBulkEntity): Long

    @Delete
    suspend fun delete(uploadBulkEntity: UploadBulkEntity)

    @Insert
    suspend fun insert(uploadBulkUriStringEntities: List<UploadBulkUriStringEntity>)

    @Transaction
    @Query(
        """
        SELECT * FROM UploadBulkEntity WHERE id = :id
    """
    )
    suspend fun get(id: Long): UploadBulkWithUri
}
