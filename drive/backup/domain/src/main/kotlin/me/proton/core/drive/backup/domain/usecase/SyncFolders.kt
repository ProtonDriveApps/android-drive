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

package me.proton.core.drive.backup.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.base.domain.util.coRunCatching
import javax.inject.Inject

class SyncFolders @Inject constructor(
    private val getFolders: GetFolders,
    private val backupManager: BackupManager,
) {
    suspend operator fun invoke(userId: UserId, uploadPriority: Long) = coRunCatching {
        getFolders(userId).getOrThrow().onEach { backupFolder ->
            backupManager.sync(userId, backupFolder, uploadPriority)
        }
    }

    suspend operator fun invoke(
        userId: UserId,
        bucketIds: List<Int>,
        uploadPriority: Long,
    ) = coRunCatching {
        getFolders(userId)
            .getOrThrow()
            .filter { backupFolder -> backupFolder.bucketId in bucketIds }
            .onEach { backupFolder ->
                backupManager.sync(userId, backupFolder, uploadPriority)
            }
    }
}
