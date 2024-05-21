/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.drivelink.shared.data.manager

import androidx.work.WorkManager
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.drivelink.shared.data.worker.MigrateKeyPacketWorker
import me.proton.core.drive.drivelink.shared.domain.manager.ShareWorkManager
import me.proton.core.drive.share.domain.repository.MigrationKeyPacketRepository
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class ShareWorkManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val repository: MigrationKeyPacketRepository,
) : ShareWorkManager {
    override suspend fun migrate(userId: UserId) {
        val lastUpdate = repository.getLastUpdate(userId)
        if (lastUpdate == null) {
            workManager.enqueue(MigrateKeyPacketWorker.getWorkRequest(userId))
        } else {
            CoreLogger.d(SHARING, "Migration is already done ($lastUpdate)")
        }
    }
}
