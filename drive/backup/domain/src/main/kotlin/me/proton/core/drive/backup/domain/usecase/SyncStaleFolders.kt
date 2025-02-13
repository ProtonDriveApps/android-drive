/*
 * Copyright (c) 2023-2024 Proton AG.
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
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class SyncStaleFolders(
    private val getAllFolders: GetAllFolders,
    private val syncFolders: SyncFolders,
    private val configurationProvider: ConfigurationProvider,
    private val clock: () -> TimestampS,
) {

    @Inject
    constructor(
        getAllFolders: GetAllFolders,
        syncFolders: SyncFolders,
        configurationProvider: ConfigurationProvider,
    ) : this(getAllFolders, syncFolders, configurationProvider, ::TimestampS)

    private val stale = { backupFolder: BackupFolder ->
        backupFolder.syncTime?.let { syncTime ->
            val duration = (clock().value - syncTime.value).seconds
            duration.isPositive() && duration > configurationProvider.backupSyncWindow
        } ?: false
    }

    suspend operator fun invoke(folderId: FolderId, uploadPriority: Long) = coRunCatching {
        getAllFolders(folderId).sync(uploadPriority)
    }

    suspend operator fun invoke(userId: UserId, uploadPriority: Long) = coRunCatching {
        getAllFolders(userId).sync(uploadPriority)
    }

    private suspend fun Result<List<BackupFolder>>.sync(
        uploadPriority: Long,
    ) = getOrThrow().filter(stale).onEach { backupFolder ->
        syncFolders(backupFolder, uploadPriority)
    }
}
