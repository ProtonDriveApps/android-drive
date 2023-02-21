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
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.trash.data.db.DriveLinkTrashDatabase
import me.proton.core.drive.drivelink.trash.domain.repository.DriveLinkTrashRepository
import javax.inject.Inject
import me.proton.core.drive.drivelink.data.extension.toDriveLinks
import me.proton.core.drive.share.domain.entity.ShareId

class DriveLinkTrashRepositoryImpl @Inject constructor(
    private val driveLinkTrashDatabase: DriveLinkTrashDatabase,
) : DriveLinkTrashRepository {

    override fun getTrashDriveLinks(shareId: ShareId): Flow<List<DriveLink>> =
        driveLinkTrashDatabase.driveLinkTrashDao.getTrashLinks(shareId.userId, shareId.id).map { entities ->
            entities.toDriveLinks()
        }
}
