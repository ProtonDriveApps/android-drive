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

package me.proton.core.drive.drivelink.offline.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import javax.inject.Inject
import me.proton.core.drive.drivelink.data.extension.toDriveLinks
import me.proton.core.drive.drivelink.offline.data.db.DriveLinkOfflineDatabase
import me.proton.core.drive.drivelink.offline.domain.repository.DriveLinkOfflineRepository

class DriveLinkOfflineRepositoryImpl @Inject constructor(
    private val db: DriveLinkOfflineDatabase,
) : DriveLinkOfflineRepository {

    override fun getOfflineDriveLinksCount(userId: UserId): Flow<Int> =
        db.driveLinkOfflineDao.getOfflineLinksCountFlow(userId)

    override fun getOfflineDriveLinks(userId: UserId, fromIndex: Int, count: Int): Flow<Result<List<DriveLink>>> =
        db.driveLinkOfflineDao.getOfflineLinks(userId, count, fromIndex).map { entities ->
            coRunCatching {
                entities.toDriveLinks()
            }
        }
}
