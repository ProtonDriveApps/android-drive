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

package me.proton.core.drive.drivelink.trash.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.trash.data.db.DriveLinkTrashDatabase
import me.proton.core.drive.drivelink.trash.domain.repository.DriveLinkTrashRepository
import javax.inject.Inject
import me.proton.core.drive.drivelink.data.extension.toDriveLinks
import me.proton.core.drive.volume.domain.entity.VolumeId

class DriveLinkTrashRepositoryImpl @Inject constructor(
    private val db: DriveLinkTrashDatabase,
) : DriveLinkTrashRepository {

    override fun getTrashDriveLinks(
        userId: UserId,
        volumeId: VolumeId,
        fromIndex: Int,
        count: Int,
    ): Flow<List<DriveLink>> =
        db.driveLinkTrashDao.getTrashLinks(userId, count, fromIndex).map { entities ->
            entities.toDriveLinks()
        }

    override fun getTrashDriveLinksCount(userId: UserId, volumeId: VolumeId): Flow<Int> =
        db.driveLinkTrashDao.getTrashedLinksCount(userId)
}
