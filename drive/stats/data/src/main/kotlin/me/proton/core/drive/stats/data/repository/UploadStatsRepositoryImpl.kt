/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.stats.data.repository

import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.stats.data.db.StatsDatabase
import me.proton.core.drive.stats.data.extension.toUploadStats
import me.proton.core.drive.stats.data.extension.toUploadStatsEntity
import me.proton.core.drive.stats.domain.entity.UploadStats
import me.proton.core.drive.stats.domain.repository.UploadStatsRepository
import javax.inject.Inject

class UploadStatsRepositoryImpl @Inject constructor(
    database: StatsDatabase,
) : UploadStatsRepository {
    private val dao = database.uploadStatsDao

    override suspend fun update(progress: UploadStats): UploadStats =
        dao.aggregate(progress.toUploadStatsEntity()).toUploadStats()

    override suspend fun get(folderId: FolderId): UploadStats? = dao.get(
        userId = folderId.userId,
        shareId = folderId.shareId.id,
        folderId = folderId.id
    )?.toUploadStats()

    override suspend fun delete(folderId: FolderId) {
        dao.delete(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id
        )
    }
}
